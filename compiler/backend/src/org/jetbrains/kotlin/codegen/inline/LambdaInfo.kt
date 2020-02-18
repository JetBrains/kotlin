/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.*
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.coroutines.isCapturedSuspendLambda
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import kotlin.properties.Delegates

interface FunctionalArgument

abstract class LambdaInfo(@JvmField val isCrossInline: Boolean) : FunctionalArgument {

    abstract val isBoundCallableReference: Boolean

    abstract val isSuspend: Boolean

    abstract val lambdaClassType: Type

    abstract val invokeMethod: Method

    abstract val invokeMethodDescriptor: FunctionDescriptor

    abstract val capturedVars: List<CapturedParamDesc>

    open val returnLabels: Map<String, Label?>
        get() = mapOf()

    lateinit var node: SMAPAndMethodNode

    abstract fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner<*>)

    open val hasDispatchReceiver = true

    fun addAllParameters(remapper: FieldRemapper): Parameters {
        val builder = ParametersBuilder.initializeBuilderFrom(OBJECT_TYPE, invokeMethod.descriptor, this)

        for (info in capturedVars) {
            val field = remapper.findField(FieldInsnNode(0, info.containingLambdaName, info.fieldName, ""))
                ?: error("Captured field not found: " + info.containingLambdaName + "." + info.fieldName)
            val recapturedParamInfo = builder.addCapturedParam(field, info.fieldName)
            if (this is ExpressionLambda && isCapturedSuspend(info)) {
                recapturedParamInfo.functionalArgument = NonInlineableArgumentForInlineableParameterCalledInSuspend
            }
        }

        return builder.buildParameters()
    }


    companion object {
        fun LambdaInfo.getCapturedParamInfo(descriptor: EnclosedValueDescriptor): CapturedParamDesc {
            return capturedParamDesc(descriptor.fieldName, descriptor.type)
        }

        fun LambdaInfo.capturedParamDesc(fieldName: String, fieldType: Type): CapturedParamDesc {
            return CapturedParamDesc(lambdaClassType, fieldName, fieldType)
        }
    }
}

object NonInlineableArgumentForInlineableParameterCalledInSuspend : FunctionalArgument
object NonInlineableArgumentForInlineableSuspendParameter : FunctionalArgument


class PsiDefaultLambda(
    lambdaClassType: Type,
    capturedArgs: Array<Type>,
    parameterDescriptor: ValueParameterDescriptor,
    offset: Int,
    needReification: Boolean
) : DefaultLambda(lambdaClassType, capturedArgs, parameterDescriptor, offset, needReification) {
    override fun mapAsmSignature(sourceCompiler: SourceCompilerForInline): Method {
        return sourceCompiler.state.typeMapper.mapSignatureSkipGeneric(invokeMethodDescriptor).asmMethod
    }

    override fun findInvokeMethodDescriptor(): FunctionDescriptor =
        parameterDescriptor.type.memberScope
            .getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND)
            .single()
}

abstract class DefaultLambda(
    override val lambdaClassType: Type,
    private val capturedArgs: Array<Type>,
    val parameterDescriptor: ValueParameterDescriptor,
    val offset: Int,
    val needReification: Boolean
) : LambdaInfo(parameterDescriptor.isCrossinline) {

    final override var isBoundCallableReference by Delegates.notNull<Boolean>()
        private set

    val parameterOffsetsInDefault: MutableList<Int> = arrayListOf()

    final override lateinit var invokeMethod: Method
        private set

    override lateinit var invokeMethodDescriptor: FunctionDescriptor

    final override lateinit var capturedVars: List<CapturedParamDesc>
        private set

    var originalBoundReceiverType: Type? = null
        private set

    override val isSuspend = parameterDescriptor.isSuspendLambda

    override fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner<*>) {
        val classBytes = loadClassBytesByInternalName(sourceCompiler.state, lambdaClassType.internalName)
        val classReader = ClassReader(classBytes)
        var isPropertyReference = false
        var isFunctionReference = false
        classReader.accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                isPropertyReference = superName in PROPERTY_REFERENCE_SUPER_CLASSES
                isFunctionReference = superName == FUNCTION_REFERENCE.internalName || superName == FUNCTION_REFERENCE_IMPL.internalName

                super.visit(version, access, name, signature, superName, interfaces)
            }
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)

        invokeMethodDescriptor = findInvokeMethodDescriptor().let {
            //property reference generates erased 'get' method
            if (isPropertyReference) it.original else it
        }

        val descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, *capturedArgs)
        val constructor = getMethodNode(classBytes, "<init>", descriptor, lambdaClassType)?.node

        assert(constructor != null || capturedArgs.isEmpty()) {
            "Can't find non-default constructor <init>$descriptor for default lambda $lambdaClassType"
        }

        capturedVars =
            if (isFunctionReference || isPropertyReference)
                constructor?.desc?.let { Type.getArgumentTypes(it) }?.singleOrNull()?.let {
                    originalBoundReceiverType = it
                    listOf(capturedParamDesc(AsmUtil.RECEIVER_PARAMETER_NAME, it.boxReceiverForBoundReference()))
                } ?: emptyList()
            else
                constructor?.findCapturedFieldAssignmentInstructions()?.map { fieldNode ->
                    capturedParamDesc(fieldNode.name, Type.getType(fieldNode.desc))
                }?.toList() ?: emptyList()

        isBoundCallableReference = (isFunctionReference || isPropertyReference) && capturedVars.isNotEmpty()

        val methodName = (if (isPropertyReference) OperatorNameConventions.GET else OperatorNameConventions.INVOKE).asString()

        val signature = mapAsmSignature(sourceCompiler)

        node = getMethodNode(classBytes, methodName, signature.descriptor, lambdaClassType, signatureAmbiguity = true)
            ?: error("Can't find method '$methodName$signature' in '${classReader.className}'")

        invokeMethod = Method(node.node.name, node.node.desc)

        if (needReification) {
            //nested classes could also require reification
            reifiedTypeInliner.reifyInstructions(node.node)
        }
    }

    protected abstract fun mapAsmSignature(sourceCompiler: SourceCompilerForInline): Method

    protected abstract fun findInvokeMethodDescriptor(): FunctionDescriptor

    private companion object {
        val PROPERTY_REFERENCE_SUPER_CLASSES =
            listOf(
                PROPERTY_REFERENCE0, PROPERTY_REFERENCE1, PROPERTY_REFERENCE2,
                MUTABLE_PROPERTY_REFERENCE0, MUTABLE_PROPERTY_REFERENCE1, MUTABLE_PROPERTY_REFERENCE2
            ).plus(OPTIMIZED_PROPERTY_REFERENCE_SUPERTYPES)
                .mapTo(HashSet(), Type::getInternalName)
    }
}

internal fun Type.boxReceiverForBoundReference() =
    AsmUtil.boxType(this)

internal fun Type.boxReceiverForBoundReference(kotlinType: KotlinType, typeMapper: KotlinTypeMapper) =
    DescriptorAsmUtil.boxType(this, kotlinType, typeMapper)

abstract class ExpressionLambda(isCrossInline: Boolean) : LambdaInfo(isCrossInline) {
    override fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner<*>) {
        node = sourceCompiler.generateLambdaBody(this)
        node.node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
    }

    abstract fun getInlineSuspendLambdaViewDescriptor(): FunctionDescriptor
    abstract fun isCapturedSuspend(desc: CapturedParamDesc): Boolean
}

class PsiExpressionLambda(
    expression: KtExpression,
    private val typeMapper: KotlinTypeMapper,
    private val languageVersionSettings: LanguageVersionSettings,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean
) : ExpressionLambda(isCrossInline) {

    override val lambdaClassType: Type

    override val invokeMethod: Method

    override val invokeMethodDescriptor: FunctionDescriptor

    val classDescriptor: ClassDescriptor

    val propertyReferenceInfo: PropertyReferenceInfo?

    val functionWithBodyOrCallableReference: KtExpression = (expression as? KtLambdaExpression)?.functionLiteral ?: expression

    override val returnLabels: Map<String, Label?>

    override val isSuspend: Boolean

    var closure: CalculatedClosure
        private set

    init {
        val bindingContext = typeMapper.bindingContext
        val function =
            bindingContext.get<PsiElement, SimpleFunctionDescriptor>(BindingContext.FUNCTION, functionWithBodyOrCallableReference)
        if (function == null && expression is KtCallableReferenceExpression) {
            val variableDescriptor =
                bindingContext.get(BindingContext.VARIABLE, functionWithBodyOrCallableReference) as? VariableDescriptorWithAccessors
                    ?: throw AssertionError("Reference expression not resolved to variable descriptor with accessors: ${expression.getText()}")
            classDescriptor = bindingContext.get(CLASS_FOR_CALLABLE, variableDescriptor)
                ?: throw IllegalStateException("Class for callable not found: $variableDescriptor\n${expression.text}")
            lambdaClassType = typeMapper.mapClass(classDescriptor)
            val getFunction = PropertyReferenceCodegen.findGetFunction(variableDescriptor)
            invokeMethodDescriptor = PropertyReferenceCodegen.createFakeOpenDescriptor(getFunction, classDescriptor)
            val resolvedCall = expression.callableReference.getResolvedCallWithAssert(bindingContext)
            propertyReferenceInfo = PropertyReferenceInfo(
                resolvedCall.resultingDescriptor as VariableDescriptor, getFunction
            )
        } else {
            propertyReferenceInfo = null
            invokeMethodDescriptor = function ?: throw AssertionError("Function is not resolved to descriptor: " + expression.text)
            classDescriptor = bindingContext.get(CLASS_FOR_CALLABLE, invokeMethodDescriptor)
                ?: throw IllegalStateException("Class for invoke method not found: $invokeMethodDescriptor\n${expression.text}")
            lambdaClassType = asmTypeForAnonymousClass(bindingContext, invokeMethodDescriptor)
        }

        bindingContext.get<ClassDescriptor, MutableClosure>(CLOSURE, classDescriptor).let {
            assert(it != null) { "Closure for lambda should be not null " + expression.text }
            closure = it!!
        }

        returnLabels = InlineCodegen.getDeclarationLabels(expression, invokeMethodDescriptor).associateWith { null }
        invokeMethod = typeMapper.mapAsmMethod(invokeMethodDescriptor)
        isSuspend = invokeMethodDescriptor.isSuspend
    }

    override val capturedVars: List<CapturedParamDesc> by lazy {
        arrayListOf<CapturedParamDesc>().apply {
            val captureThis = closure.capturedOuterClassDescriptor
            if (captureThis != null) {
                val kotlinType = captureThis.defaultType
                val type = typeMapper.mapType(kotlinType)
                val descriptor = EnclosedValueDescriptor(
                    AsmUtil.CAPTURED_THIS_FIELD, null,
                    StackValue.field(type, lambdaClassType, AsmUtil.CAPTURED_THIS_FIELD, false, StackValue.LOCAL_0),
                    type, kotlinType
                )
                add(getCapturedParamInfo(descriptor))
            }

            val capturedReceiver = closure.capturedReceiverFromOuterContext
            if (capturedReceiver != null) {
                val type = typeMapper.mapType(capturedReceiver).let {
                    if (isBoundCallableReference) it.boxReceiverForBoundReference() else it
                }

                val fieldName = closure.getCapturedReceiverFieldName(typeMapper.bindingContext, languageVersionSettings)
                val descriptor = EnclosedValueDescriptor(
                    fieldName, null,
                    StackValue.field(type, capturedReceiver, lambdaClassType, fieldName, false, StackValue.LOCAL_0),
                    type, capturedReceiver
                )
                add(getCapturedParamInfo(descriptor))
            }

            closure.captureVariables.values.forEach { descriptor ->
                add(getCapturedParamInfo(descriptor))
            }
        }
    }

    val isPropertyReference: Boolean
        get() = propertyReferenceInfo != null

    override fun getInlineSuspendLambdaViewDescriptor(): FunctionDescriptor {
        return getOrCreateJvmSuspendFunctionView(
            invokeMethodDescriptor,
            languageVersionSettings.isReleaseCoroutines(),
            typeMapper.bindingContext
        )
    }

    override fun isCapturedSuspend(desc: CapturedParamDesc): Boolean =
        isCapturedSuspendLambda(closure, desc.fieldName, typeMapper.bindingContext)
}
