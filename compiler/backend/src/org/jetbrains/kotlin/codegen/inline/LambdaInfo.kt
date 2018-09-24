/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.*
import org.jetbrains.kotlin.codegen.binding.MutableClosure
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.properties.Delegates

abstract class LambdaInfo(@JvmField val isCrossInline: Boolean) : LabelOwner {

    abstract val isBoundCallableReference: Boolean

    abstract val lambdaClassType: Type

    abstract val invokeMethod: Method

    abstract val invokeMethodDescriptor: FunctionDescriptor

    abstract val capturedVars: List<CapturedParamDesc>

    lateinit var node: SMAPAndMethodNode

    abstract fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner)

    open val hasDispatchReceiver = true

    fun addAllParameters(remapper: FieldRemapper): Parameters {
        val builder = ParametersBuilder.initializeBuilderFrom(AsmTypes.OBJECT_TYPE, invokeMethod.descriptor, this)

        for (info in capturedVars) {
            val field = remapper.findField(FieldInsnNode(0, info.containingLambdaName, info.fieldName, ""))
                    ?: error("Captured field not found: " + info.containingLambdaName + "." + info.fieldName)
            builder.addCapturedParam(field, info.fieldName)
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


class DefaultLambda(
    override val lambdaClassType: Type,
    private val capturedArgs: Array<Type>,
    val parameterDescriptor: ValueParameterDescriptor,
    val offset: Int,
    val needReification: Boolean
) : LambdaInfo(parameterDescriptor.isCrossinline) {

    override var isBoundCallableReference by Delegates.notNull<Boolean>()
        private set

    val parameterOffsetsInDefault: MutableList<Int> = arrayListOf()

    override lateinit var invokeMethod: Method
        private set

    override lateinit var invokeMethodDescriptor: FunctionDescriptor

    override lateinit var capturedVars: List<CapturedParamDesc>
        private set

    override fun isMyLabel(name: String): Boolean = false

    var originalBoundReceiverType: Type? = null
        private set

    override fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner) {
        val classReader = buildClassReaderByInternalName(sourceCompiler.state, lambdaClassType.internalName)
        var isPropertyReference = false
        var isFunctionReference = false
        classReader.accept(object : ClassVisitor(API) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                isPropertyReference = superName?.startsWith("kotlin/jvm/internal/PropertyReference") ?: false
                isFunctionReference = "kotlin/jvm/internal/FunctionReference" == superName

                super.visit(version, access, name, signature, superName, interfaces)
            }
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)

        invokeMethodDescriptor =
                parameterDescriptor.type.memberScope
                    .getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND)
                    .single()
                    .let {
                        //property reference generates erased 'get' method
                        if (isPropertyReference) it.original else it
                    }

        val descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, *capturedArgs)
        val constructor = getMethodNode(
            classReader.b,
            "<init>",
            descriptor,
            lambdaClassType
        )?.node

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

        invokeMethod = Method(
            (if (isPropertyReference) OperatorNameConventions.GET else OperatorNameConventions.INVOKE).asString(),
            sourceCompiler.state.typeMapper.mapSignatureSkipGeneric(invokeMethodDescriptor).asmMethod.descriptor
        )

        node = getMethodNode(
            classReader.b,
            invokeMethod.name,
            invokeMethod.descriptor,
            lambdaClassType
        ) ?: error("Can't find method '${invokeMethod.name}${invokeMethod.descriptor}' in '${classReader.className}'")

        if (needReification) {
            //nested classes could also require reification
            reifiedTypeInliner.reifyInstructions(node.node)
        }
    }
}

fun Type.boxReceiverForBoundReference() = AsmUtil.boxType(this)

abstract class ExpressionLambda(protected val typeMapper: KotlinTypeMapper, isCrossInline: Boolean) : LambdaInfo(isCrossInline) {

    override fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner) {
        val jvmMethodSignature = typeMapper.mapSignatureSkipGeneric(invokeMethodDescriptor)
        val asmMethod = jvmMethodSignature.asmMethod
        val methodNode = MethodNode(
            API, AsmUtil.getMethodAsmFlags(invokeMethodDescriptor, OwnerKind.IMPLEMENTATION, sourceCompiler.state),
            asmMethod.name, asmMethod.descriptor, null, null
        )

        node = wrapWithMaxLocalCalc(methodNode).let { adapter ->
            val smap = sourceCompiler.generateLambdaBody(
                adapter, jvmMethodSignature, this
            )
            adapter.visitMaxs(-1, -1)
            SMAPAndMethodNode(methodNode, smap)
        }
    }
}

class PsiExpressionLambda(
    expression: KtExpression,
    typeMapper: KotlinTypeMapper,
    languageVersionSettings: LanguageVersionSettings,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean
) : ExpressionLambda(typeMapper, isCrossInline) {

    override val lambdaClassType: Type

    override val invokeMethod: Method

    override val invokeMethodDescriptor: FunctionDescriptor

    val classDescriptor: ClassDescriptor

    val propertyReferenceInfo: PropertyReferenceInfo?

    val functionWithBodyOrCallableReference: KtExpression = (expression as? KtLambdaExpression)?.functionLiteral ?: expression

    private val labels: Set<String>

    private lateinit var closure: CalculatedClosure

    init {
        val bindingContext = typeMapper.bindingContext
        val function =
            bindingContext.get<PsiElement, SimpleFunctionDescriptor>(BindingContext.FUNCTION, functionWithBodyOrCallableReference)
        if (function == null && expression is KtCallableReferenceExpression) {
            val variableDescriptor =
                bindingContext.get(BindingContext.VARIABLE, functionWithBodyOrCallableReference) as? VariableDescriptorWithAccessors
                        ?: throw AssertionError("""Reference expression not resolved to variable descriptor with accessors: ${expression.getText()}""")
            classDescriptor = CodegenBinding.anonymousClassForCallable(bindingContext, variableDescriptor)
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
            classDescriptor = anonymousClassForCallable(bindingContext, invokeMethodDescriptor)
            lambdaClassType = asmTypeForAnonymousClass(bindingContext, invokeMethodDescriptor)
        }

        bindingContext.get<ClassDescriptor, MutableClosure>(CLOSURE, classDescriptor).let {
            assert(it != null) { "Closure for lambda should be not null " + expression.text }
            closure = it!!
        }

        labels = InlineCodegen.getDeclarationLabels(expression, invokeMethodDescriptor)
        invokeMethod = typeMapper.mapAsmMethod(invokeMethodDescriptor)
    }

    override val capturedVars: List<CapturedParamDesc> by lazy {
        arrayListOf<CapturedParamDesc>().apply {
            val captureThis = closure.capturedOuterClassDescriptor
            if (captureThis != null) {
                val type = typeMapper.mapType(captureThis)
                val descriptor = EnclosedValueDescriptor(
                    AsmUtil.CAPTURED_THIS_FIELD, null,
                    StackValue.field(type, lambdaClassType, AsmUtil.CAPTURED_THIS_FIELD, false, StackValue.LOCAL_0),
                    type
                )
                add(getCapturedParamInfo(descriptor))
            }

            if (closure.capturedReceiverFromOuterContext != null) {
                val type = typeMapper.mapType(closure.capturedReceiverFromOuterContext!!).let {
                    if (isBoundCallableReference) it.boxReceiverForBoundReference() else it
                }

                val fieldName = closure.getCapturedReceiverFieldName(typeMapper.bindingContext, languageVersionSettings)
                val descriptor = EnclosedValueDescriptor(
                    fieldName, null,
                    StackValue.field(type, lambdaClassType, fieldName, false, StackValue.LOCAL_0),
                    type
                )
                add(getCapturedParamInfo(descriptor))
            }

            closure.captureVariables.values.forEach { descriptor ->
                add(getCapturedParamInfo(descriptor))
            }
        }
    }

    override fun isMyLabel(name: String): Boolean {
        return labels.contains(name)
    }

    val isPropertyReference: Boolean
        get() = propertyReferenceInfo != null
}