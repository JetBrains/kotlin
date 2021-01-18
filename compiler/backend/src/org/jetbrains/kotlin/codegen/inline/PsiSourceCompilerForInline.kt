/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.*
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.jvm.annotations.isCallableMemberCompiledToJvmDefault
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.expressions.LabelResolver
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class PsiSourceCompilerForInline(
    private val codegen: ExpressionCodegen,
    override val callElement: KtElement,
    private val functionDescriptor: FunctionDescriptor
) : SourceCompilerForInline {
    override val state
        get() = codegen.state

    private val additionalInnerClasses = mutableListOf<ClassDescriptor>()

    val context = getContext(
        functionDescriptor,
        functionDescriptor,
        codegen.state,
        DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor)?.containingFile as? KtFile,
        additionalInnerClasses
    ) as MethodContext

    override val callElementText: String by lazy { callElement.text }

    override val inlineCallSiteInfo: InlineCallSiteInfo
        get() {
            var context = codegen.getContext()
            var parentCodegen = codegen.parentCodegen
            while (context is InlineLambdaContext) {
                val closureContext = context.getParentContext()
                assert(closureContext is ClosureContext) { "Parent context of inline lambda should be closure context" }
                assert(closureContext.parentContext is MethodContext) { "Closure context should appear in method context" }
                context = closureContext.parentContext as MethodContext
                assert(parentCodegen is FakeMemberCodegen) { "Parent codegen of inlined lambda should be FakeMemberCodegen" }
                parentCodegen = (parentCodegen as FakeMemberCodegen).delegate
            }

            val signature = codegen.state.typeMapper.mapSignatureSkipGeneric(context.functionDescriptor, context.contextKind)
            return InlineCallSiteInfo(
                parentCodegen.className,
                signature.asmMethod,
                context.functionDescriptor.getInlineCallSiteVisibility(),
                callElement.containingFile,
                CodegenUtil.getLineNumberForElement(callElement, false) ?: 0
            )
        }

    override val sourceMapper
        get() = codegen.parentCodegen.orCreateSourceMapper

    override fun generateLambdaBody(lambdaInfo: ExpressionLambda, reifiedTypeParameters: ReifiedTypeParametersUsages): SMAPAndMethodNode {
        require(lambdaInfo is PsiExpressionLambda)
        val invokeMethodDescriptor = lambdaInfo.invokeMethodDescriptor
        val jvmMethodSignature = state.typeMapper.mapSignatureSkipGeneric(invokeMethodDescriptor)
        val asmMethod = jvmMethodSignature.asmMethod
        val methodNode = MethodNode(
            Opcodes.API_VERSION, DescriptorAsmUtil.getMethodAsmFlags(invokeMethodDescriptor, OwnerKind.IMPLEMENTATION, state),
            asmMethod.name, asmMethod.descriptor, null, null
        )
        val adapter = wrapWithMaxLocalCalc(methodNode)
        val closureContext = when {
            lambdaInfo.isPropertyReference ->
                codegen.getContext().intoAnonymousClass(lambdaInfo.classDescriptor, codegen, OwnerKind.IMPLEMENTATION)
            invokeMethodDescriptor.isSuspend ->
                codegen.getContext().intoCoroutineClosure(
                    getOrCreateJvmSuspendFunctionView(invokeMethodDescriptor, state), invokeMethodDescriptor, codegen, state.typeMapper
                )
            else -> codegen.getContext().intoClosure(invokeMethodDescriptor, codegen, state.typeMapper)
        }
        val context = closureContext.intoInlinedLambda(invokeMethodDescriptor, lambdaInfo.isCrossInline, lambdaInfo.isPropertyReference)
        val smap = generateMethodBody(
            adapter, invokeMethodDescriptor, context,
            lambdaInfo.functionWithBodyOrCallableReference,
            jvmMethodSignature, lambdaInfo
        )
        adapter.visitMaxs(-1, -1)
        return SMAPAndMethodNode(methodNode, smap)
    }

    private fun generateMethodBody(
        adapter: MethodVisitor,
        descriptor: FunctionDescriptor,
        context: MethodContext,
        expression: KtExpression,
        jvmMethodSignature: JvmMethodSignature,
        lambdaInfo: PsiExpressionLambda?
    ): SMAP {
        val isLambda = lambdaInfo != null

        // Wrapping for preventing marking actual parent codegen as containing reified markers
        val parentCodegen = FakeMemberCodegen(
            codegen.parentCodegen, expression, context.parentContext as FieldOwnerContext<*>,
            if (isLambda)
                codegen.parentCodegen.className
            else
                state.typeMapper.mapImplementationOwner(descriptor).internalName,
            if (isLambda) emptyList() else additionalInnerClasses,
            isLambda
        )

        val strategy = when (expression) {
            is KtCallableReferenceExpression -> {
                val resolvedCall = expression.callableReference.getResolvedCallWithAssert(state.bindingContext)
                val receiverKotlinType = JvmCodegenUtil.getBoundCallableReferenceReceiver(resolvedCall)?.type
                val receiverType = receiverKotlinType?.let(state.typeMapper::mapType)
                val boundReceiverJvmKotlinType = receiverType?.let { JvmKotlinType(receiverType, receiverKotlinType) }

                if (isLambda && lambdaInfo!!.isPropertyReference) {
                    val asmType = state.typeMapper.mapClass(lambdaInfo.classDescriptor)
                    val info = lambdaInfo.propertyReferenceInfo
                    PropertyReferenceCodegen.PropertyReferenceGenerationStrategy(
                        true, info!!.getFunction, info.target, asmType,
                        boundReceiverJvmKotlinType,
                        lambdaInfo.functionWithBodyOrCallableReference, state, true
                    )
                } else {
                    FunctionReferenceGenerationStrategy(state, descriptor, resolvedCall, boundReceiverJvmKotlinType, null, true)
                }
            }
            is KtFunctionLiteral -> ClosureGenerationStrategy(state, expression as KtDeclarationWithBody)
            else -> FunctionGenerationStrategy.FunctionDefault(state, expression as KtDeclarationWithBody)
        }

        FunctionCodegen.generateMethodBody(
            adapter, descriptor, context, jvmMethodSignature, strategy, parentCodegen, state.jvmDefaultMode
        )

        if (isLambda) {
            codegen.propagateChildReifiedTypeParametersUsages(parentCodegen.reifiedTypeParametersUsages)
        }

        return SMAP(parentCodegen.orCreateSourceMapper.resultMappings)
    }

    @Suppress("UNCHECKED_CAST")
    private class FakeMemberCodegen(
        val delegate: MemberCodegen<*>,
        declaration: KtElement,
        codegenContext: FieldOwnerContext<*>,
        private val className: String,
        private val parentAsInnerClasses: List<ClassDescriptor>,
        private val isInlineLambdaCodegen: Boolean
    ) : MemberCodegen<KtPureElement>(delegate as MemberCodegen<KtPureElement>, declaration, codegenContext) {

        override fun generateDeclaration() {
            throw IllegalStateException()
        }

        override fun generateBody() {
            throw IllegalStateException()
        }

        override fun generateKotlinMetadataAnnotation() {
            throw IllegalStateException()
        }

        override fun getInlineNameGenerator(): NameGenerator {
            return delegate.inlineNameGenerator
        }

        override //TODO: obtain name from context
        fun getClassName(): String {
            return className
        }

        override fun addParentsToInnerClassesIfNeeded(innerClasses: MutableCollection<ClassDescriptor>) {
            if (isInlineLambdaCodegen) {
                super.addParentsToInnerClassesIfNeeded(innerClasses)
            } else {
                innerClasses.addAll(parentAsInnerClasses)
            }
        }

        override fun generateAssertField() {
            delegate.generateAssertField()
        }
    }

    private fun getDirectMemberAndCallableFromObject(): CallableMemberDescriptor {
        val directMember = JvmCodegenUtil.getDirectMember(functionDescriptor)
        return (directMember as? ImportedFromObjectCallableDescriptor<*>)?.callableFromObject ?: directMember
    }

    internal var callDefault: Boolean = false

    private fun mapDefault(): Method {
        // This is all available in the `Callable` passed to `PsiInlineCodegen.genCallInner`, but it's not forwarded through the inliner...
        var result = state.typeMapper.mapDefaultMethod(functionDescriptor, context.contextKind)
        if (result.name.contains("-") &&
            !state.configuration.getBoolean(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME) &&
            classFileContainsMethod(functionDescriptor, state, result) == false
        ) {
            state.typeMapper.useOldManglingRulesForFunctionAcceptingInlineClass = true
            result = state.typeMapper.mapDefaultMethod(functionDescriptor, context.contextKind)
            state.typeMapper.useOldManglingRulesForFunctionAcceptingInlineClass = false
        }
        return result
    }

    override fun compileInlineFunction(jvmSignature: JvmMethodSignature): SMAPAndMethodNode {
        generateInlineIntrinsic(functionDescriptor, jvmSignature.asmMethod, codegen.typeSystem)?.let {
            return it
        }

        val asmMethod = if (callDefault) mapDefault() else jvmSignature.asmMethod
        if (asmMethod.name != functionDescriptor.name.asString()) {
            KotlinLookupLocation(callElement).location?.let {
                state.trackLookup(DescriptorUtils.getFqNameSafe(functionDescriptor.containingDeclaration), asmMethod.name, it)
            }
        }

        val directMember = getDirectMemberAndCallableFromObject()
        if (directMember is DescriptorWithContainerSource) {
            val containerId = KotlinTypeMapper.getContainingClassesForDeserializedCallable(directMember).implClassId
            val isMangled = requiresFunctionNameManglingForReturnType(functionDescriptor)
            return loadCompiledInlineFunction(containerId, asmMethod, functionDescriptor.isSuspend, isMangled, state)
        }

        val element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? KtDeclarationWithBody
            ?: throw IllegalStateException("Couldn't find declaration for function $functionDescriptor")

        val node = MethodNode(
            Opcodes.API_VERSION,
            DescriptorAsmUtil.getMethodAsmFlags(functionDescriptor, context.contextKind, state) or
                    if (callDefault) Opcodes.ACC_STATIC else 0,
            asmMethod.name,
            asmMethod.descriptor, null, null
        )

        //for maxLocals calculation
        val maxCalcAdapter = wrapWithMaxLocalCalc(node)
        val smap = if (callDefault) {
            val implementationOwner = state.typeMapper.mapImplementationOwner(functionDescriptor)
            val parentCodegen = FakeMemberCodegen(
                codegen.parentCodegen, element, context.parentContext as FieldOwnerContext<*>,
                implementationOwner.internalName,
                additionalInnerClasses,
                false
            )
            if (element !is KtNamedFunction) {
                throw IllegalStateException("Property accessors with default parameters not supported $functionDescriptor")
            }
            FunctionCodegen.generateDefaultImplBody(
                context, functionDescriptor, maxCalcAdapter, DefaultParameterValueLoader.DEFAULT,
                element as KtNamedFunction?, parentCodegen, asmMethod
            )
            SMAP(parentCodegen.orCreateSourceMapper.resultMappings)
        } else {
            generateMethodBody(maxCalcAdapter, functionDescriptor, context, element, jvmSignature, null)
        }
        maxCalcAdapter.visitMaxs(-1, -1)
        maxCalcAdapter.visitEnd()

        return SMAPAndMethodNode(node, smap)
    }

    override fun hasFinallyBlocks() = codegen.hasFinallyBlocks()

    override fun generateFinallyBlocks(finallyNode: MethodNode, curFinallyDepth: Int, returnType: Type, afterReturnLabel: Label, target: Label?) {
        // TODO use the target label for non-local break/continue
        ExpressionCodegen(
            finallyNode, codegen.frameMap, codegen.returnType,
            codegen.getContext(), codegen.state, codegen.parentCodegen
        ).also {
            it.addBlockStackElementsForNonLocalReturns(codegen.blockStackElements, curFinallyDepth)
        }.generateFinallyBlocksIfNeeded(returnType, null, afterReturnLabel)
    }

    override val isCallInsideSameModuleAsCallee: Boolean
        get() = JvmCodegenUtil.isCallInsideSameModuleAsDeclared(functionDescriptor, codegen.getContext(), codegen.state.outDirectory)

    override val isFinallyMarkerRequired: Boolean
        get() = isFinallyMarkerRequired(codegen.getContext())

    override fun isSuspendLambdaCapturedByOuterObjectOrLambda(name: String): Boolean {
        // We cannot find the lambda in captured parameters: it came from object outside of the our reach:
        // this can happen when the lambda capture by non-transformed closure:
        //   inline fun inlineMe(crossinline c: suspend() -> Unit) = suspend { c() }
        //   inline fun inlineMe2(crossinline c: suspend() -> Unit) = suspend { inlineMe { c() }() }
        // Suppose, we inline inlineMe into inlineMe2: the only knowledge we have about inlineMe$1 is captured receiver (this$0)
        // Thus, transformed lambda from inlineMe, inlineMe3$$inlined$inlineMe2$1 contains the following bytecode
        //   ALOAD 0
        //   GETFIELD inlineMe2$1$invokeSuspend$$inlined$inlineMe$1.this$0 : LScratchKt$inlineMe2$1;
        //   GETFIELD inlineMe2$1.$c : Lkotlin/jvm/functions/Function1;
        // Since inlineMe2's lambda is outside of reach of the inliner, find crossinline parameter from compilation context:
        var container: DeclarationDescriptor = codegen.getContext().functionDescriptor
        while (container !is ClassDescriptor) {
            container = container.containingDeclaration ?: return false
        }
        var classDescriptor: ClassDescriptor? = container
        while (classDescriptor != null) {
            val closure = state.bindingContext[CodegenBinding.CLOSURE, classDescriptor] ?: return false
            for ((param, value) in closure.captureVariables) {
                if (param is ValueParameterDescriptor && value.fieldName == name) {
                    return param.type.isSuspendFunctionTypeOrSubtype
                }
            }
            classDescriptor = closure.capturedOuterClassDescriptor
        }
        return false
    }

    override fun getContextLabels(): Map<String, Label?> {
        val context = codegen.getContext()
        val parentContext = context.parentContext
        val descriptor = if (parentContext is ClosureContext && parentContext.originalSuspendLambdaDescriptor != null) {
            parentContext.originalSuspendLambdaDescriptor!!
        } else context.contextDescriptor

        val labels = getDeclarationLabels(DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor)
        return labels.associateWith { null } // TODO add break/continue labels
    }

    override fun reportSuspensionPointInsideMonitor(stackTraceElement: String) {
        org.jetbrains.kotlin.codegen.coroutines.reportSuspensionPointInsideMonitor(callElement, state, stackTraceElement)
    }

    companion object {
        fun getContext(
            descriptor: DeclarationDescriptor,
            innerDescriptor: DeclarationDescriptor,
            state: GenerationState,
            sourceFile: KtFile?,
            additionalInners: MutableList<ClassDescriptor>
        ): CodegenContext<*> {
            if (descriptor is PackageFragmentDescriptor) {
                //no inners
                return PackageContext(descriptor, state.rootContext, null, sourceFile)
            }

            val container = descriptor.containingDeclaration ?: error("No container for descriptor: $descriptor")
            val containerContext = getContext(
                container,
                descriptor,
                state,
                sourceFile,
                additionalInners
            )

            return when (descriptor) {
                is ScriptDescriptor -> {
                    val earlierScripts = state.scriptSpecific.earlierScriptsForReplInterpreter
                    containerContext.intoScript(
                        descriptor,
                        earlierScripts ?: emptyList(),
                        descriptor as ClassDescriptor, state.typeMapper
                    )
                }
                is ClassDescriptor -> {
                    val kind =
                        when {
                            DescriptorUtils.isInterface(descriptor) &&
                                    innerDescriptor !is ClassDescriptor &&
                                    !innerDescriptor.isCallableMemberCompiledToJvmDefault(state.jvmDefaultMode) ->
                                OwnerKind.DEFAULT_IMPLS
                            else ->
                                OwnerKind.IMPLEMENTATION
                        }

                    additionalInners.addIfNotNull(
                        InnerClassConsumer.classForInnerClassRecord(descriptor, kind == OwnerKind.DEFAULT_IMPLS)
                    )

                    if (descriptor.isInlineClass()) {
                        containerContext.intoClass(descriptor, OwnerKind.IMPLEMENTATION, state)
                            .intoClass(descriptor, OwnerKind.ERASED_INLINE_CLASS, state)
                    } else {
                        containerContext.intoClass(descriptor, kind, state)
                    }
                }
                is FunctionDescriptor -> {
                    containerContext.intoFunction(descriptor)
                }
                else -> {
                    throw IllegalStateException("Couldn't build context for $descriptor")
                }
            }

        }
    }
}

fun DeclarationDescriptor.isInlineOrInsideInline(): Boolean =
    getInlineCallSiteVisibility() != null

fun DeclarationDescriptor.getInlineCallSiteVisibility(): DescriptorVisibility? {
    var declaration: DeclarationDescriptor? = this
    var result: DescriptorVisibility? = null
    while (declaration != null) {
        if (declaration is FunctionDescriptor && declaration.isInline) {
            if (!DescriptorVisibilities.isPrivate(declaration.visibility)) {
                return declaration.visibility
            }
            result = declaration.visibility
        }
        declaration = declaration.containingDeclaration
    }
    return result
}

fun getDeclarationLabels(lambdaOrFun: PsiElement?, descriptor: DeclarationDescriptor): Set<String> {
    val result = HashSet<String>()

    if (lambdaOrFun != null) {
        val label = LabelResolver.getLabelNamesIfAny(lambdaOrFun, addContextReceiverNames = false)
        if (label.isNotEmpty()) {
            result.add(label.single().asString())
        }
    }

    if (!ExpressionTypingUtils.isFunctionLiteral(descriptor)) {
        if (!descriptor.name.isSpecial) {
            result.add(descriptor.name.asString())
        }
        result.add(FIRST_FUN_LABEL)
    }
    return result
}