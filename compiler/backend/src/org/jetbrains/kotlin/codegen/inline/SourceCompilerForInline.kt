/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.*
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.jvm.annotations.isCallableMemberWithJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.properties.Delegates

interface SourceCompilerForInline {
    val state: GenerationState

    val callElement: Any

    val lookupLocation: LookupLocation

    val callElementText: String

    val callsiteFile: PsiFile?

    val contextKind: OwnerKind

    val inlineCallSiteInfo: InlineCallSiteInfo

    val lazySourceMapper: DefaultSourceMapper

    fun generateLambdaBody(lambdaInfo: ExpressionLambda): SMAPAndMethodNode

    fun doCreateMethodNodeFromSource(
        callableDescriptor: FunctionDescriptor,
        jvmSignature: JvmMethodSignature,
        callDefault: Boolean,
        asmMethod: Method
    ): SMAPAndMethodNode

    fun hasFinallyBlocks(): Boolean

    fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(
        finallyNode: MethodNode,
        curFinallyDepth: Int
    ): BaseExpressionCodegen

    fun generateFinallyBlocksIfNeeded(finallyCodegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label)

    fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean

    fun isFinallyMarkerRequired(): Boolean

    val compilationContextDescriptor: DeclarationDescriptor

    val compilationContextFunctionDescriptor: FunctionDescriptor

    fun getContextLabels(): Set<String>

    fun initializeInlineFunctionContext(functionDescriptor: FunctionDescriptor)
}


class PsiSourceCompilerForInline(private val codegen: ExpressionCodegen, override val callElement: KtElement) :
    SourceCompilerForInline {

    override val state = codegen.state

    private var context by Delegates.notNull<CodegenContext<*>>()

    private var additionalInnerClasses = mutableListOf<ClassDescriptor>()

    override val lookupLocation = KotlinLookupLocation(callElement)


    override val callElementText: String by lazy {
        callElement.text
    }

    override val callsiteFile by lazy {
        callElement.containingFile
    }

    override val contextKind
        get () = context.contextKind

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
                signature.asmMethod.name,
                signature.asmMethod.descriptor,
                compilationContextFunctionDescriptor.isInlineOrInsideInline(),
                compilationContextFunctionDescriptor.isSuspend
            )
        }

    override val lazySourceMapper
        get() = codegen.parentCodegen.orCreateSourceMapper

    override fun generateLambdaBody(lambdaInfo: ExpressionLambda): SMAPAndMethodNode {
        lambdaInfo as? PsiExpressionLambda ?: error("TODO")
        val invokeMethodDescriptor = lambdaInfo.invokeMethodDescriptor
        val jvmMethodSignature = state.typeMapper.mapSignatureSkipGeneric(invokeMethodDescriptor)
        val asmMethod = jvmMethodSignature.asmMethod
        val methodNode = MethodNode(
            Opcodes.API_VERSION, AsmUtil.getMethodAsmFlags(invokeMethodDescriptor, OwnerKind.IMPLEMENTATION, state),
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
            if (isLambda) emptyList<ClassDescriptor>() else additionalInnerClasses,
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
            adapter, descriptor, context, jvmMethodSignature, strategy, parentCodegen, state.jvmDefaultMode,
            state.languageVersionSettings.isReleaseCoroutines()
        )

        if (isLambda) {
            codegen.propagateChildReifiedTypeParametersUsages(parentCodegen.reifiedTypeParametersUsages)
        }

        return createSMAPWithDefaultMapping(expression, parentCodegen.orCreateSourceMapper.resultMappings)
    }


    private fun createSMAPWithDefaultMapping(
        declaration: KtExpression,
        mappings: List<FileMapping>
    ): SMAP {
        val containingFile = declaration.containingFile
        CodegenUtil.getLineNumberForElement(containingFile, true) ?: error("Couldn't extract line count in $containingFile")

        return SMAP(mappings)
    }

    @Suppress("UNCHECKED_CAST")
    private class FakeMemberCodegen(
        internal val delegate: MemberCodegen<*>,
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

    override fun doCreateMethodNodeFromSource(
        callableDescriptor: FunctionDescriptor,
        jvmSignature: JvmMethodSignature,
        callDefault: Boolean,
        asmMethod: Method
    ): SMAPAndMethodNode {
        val element = DescriptorToSourceUtils.descriptorToDeclaration(callableDescriptor)

        if (!(element is KtNamedFunction || element is KtPropertyAccessor)) {
            throw IllegalStateException("Couldn't find declaration for function $callableDescriptor")
        }
        val inliningFunction = element as KtDeclarationWithBody?

        val node = MethodNode(
            Opcodes.API_VERSION,
            AsmUtil.getMethodAsmFlags(callableDescriptor, context.contextKind, state) or if (callDefault) Opcodes.ACC_STATIC else 0,
            asmMethod.name,
            asmMethod.descriptor, null, null
        )

        //for maxLocals calculation
        val maxCalcAdapter = wrapWithMaxLocalCalc(node)
        val parentContext = context.parentContext ?: error("Context has no parent: " + context)
        val methodContext = parentContext.intoFunction(callableDescriptor)

        val smap = if (callDefault) {
            val implementationOwner = state.typeMapper.mapImplementationOwner(callableDescriptor)
            val parentCodegen = FakeMemberCodegen(
                codegen.parentCodegen, inliningFunction!!, methodContext.parentContext as FieldOwnerContext<*>,
                implementationOwner.internalName,
                additionalInnerClasses,
                false
            )
            if (element !is KtNamedFunction) {
                throw IllegalStateException("Property accessors with default parameters not supported $callableDescriptor")
            }
            FunctionCodegen.generateDefaultImplBody(
                methodContext, callableDescriptor, maxCalcAdapter, DefaultParameterValueLoader.DEFAULT,
                inliningFunction as KtNamedFunction?, parentCodegen, asmMethod
            )
            createSMAPWithDefaultMapping(inliningFunction, parentCodegen.orCreateSourceMapper.resultMappings)
        } else {
            generateMethodBody(maxCalcAdapter, callableDescriptor, methodContext, inliningFunction!!, jvmSignature, null)
        }
        maxCalcAdapter.visitMaxs(-1, -1)
        maxCalcAdapter.visitEnd()

        return SMAPAndMethodNode(node, smap)
    }

    override fun hasFinallyBlocks() = codegen.hasFinallyBlocks()

    override fun generateFinallyBlocksIfNeeded(finallyCodegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label) {
        require(finallyCodegen is ExpressionCodegen)
        finallyCodegen.generateFinallyBlocksIfNeeded(returnType, null, afterReturnLabel)
    }

    override fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode: MethodNode, curFinallyDepth: Int) =
        ExpressionCodegen(
            finallyNode, codegen.frameMap, codegen.returnType,
            codegen.getContext(), codegen.state, codegen.parentCodegen
        ).also {
            it.addBlockStackElementsForNonLocalReturns(codegen.blockStackElements, curFinallyDepth)
        }

    override fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean {
        return JvmCodegenUtil.isCallInsideSameModuleAsDeclared(functionDescriptor, codegen.getContext(), codegen.state.outDirectory)
    }

    override fun isFinallyMarkerRequired(): Boolean = isFinallyMarkerRequired(codegen.getContext())


    override val compilationContextDescriptor
        get() = codegen.getContext().contextDescriptor

    override val compilationContextFunctionDescriptor
        get() = codegen.getContext().functionDescriptor

    override fun getContextLabels(): Set<String> {
        val context = codegen.getContext()
        val parentContext = context.parentContext
        val descriptor = if (parentContext is ClosureContext && parentContext.originalSuspendLambdaDescriptor != null) {
            parentContext.originalSuspendLambdaDescriptor!!
        } else context.contextDescriptor

        return InlineCodegen.getDeclarationLabels(DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor)
    }

    override fun initializeInlineFunctionContext(functionDescriptor: FunctionDescriptor) {
        context = getContext(
            functionDescriptor,
            functionDescriptor,
            state,
            DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor)?.containingFile as? KtFile,
            additionalInnerClasses
        )
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
                    val earlierScripts = state.replSpecific.earlierScriptsForReplInterpreter
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
                                    !innerDescriptor.isCallableMemberWithJvmDefaultAnnotation() ->
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

private fun DeclarationDescriptor.isInlineOrInsideInline(): Boolean =
    if (this is FunctionDescriptor && isInline) true
    else containingDeclaration?.isInlineOrInsideInline() == true
