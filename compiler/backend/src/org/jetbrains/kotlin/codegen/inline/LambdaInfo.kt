/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode
import kotlin.properties.Delegates

interface FunctionalArgument

abstract class LambdaInfo(@JvmField val isCrossInline: Boolean) : FunctionalArgument {

    abstract val isBoundCallableReference: Boolean

    abstract val isSuspend: Boolean

    abstract val lambdaClassType: Type

    abstract val invokeMethod: Method

    abstract val invokeMethodParameters: List<KotlinType?>

    abstract val invokeMethodReturnType: KotlinType?

    abstract val capturedVars: List<CapturedParamDesc>

    open val returnLabels: Map<String, Label?>
        get() = mapOf()

    lateinit var node: SMAPAndMethodNode

    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()

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

abstract class ExpressionLambda(isCrossInline: Boolean) : LambdaInfo(isCrossInline) {
    override fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner<*>) {
        node = sourceCompiler.generateLambdaBody(this, reifiedTypeParametersUsages)
        node.node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
    }

    abstract fun isCapturedSuspend(desc: CapturedParamDesc): Boolean
}

abstract class DefaultLambda(
    private val capturedArgs: Array<Type>,
    isCrossinline: Boolean,
    val offset: Int,
    val needReification: Boolean
) : LambdaInfo(isCrossinline) {

    final override var isBoundCallableReference by Delegates.notNull<Boolean>()
        private set

    val parameterOffsetsInDefault: MutableList<Int> = arrayListOf()

    final override lateinit var invokeMethod: Method
        private set

    private lateinit var invokeMethodDescriptor: FunctionDescriptor

    override val invokeMethodParameters: List<KotlinType?>
        get() = invokeMethodDescriptor.valueParameters.map { it.returnType }

    override val invokeMethodReturnType: KotlinType?
        get() = invokeMethodDescriptor.returnType

    final override lateinit var capturedVars: List<CapturedParamDesc>
        private set

    var originalBoundReceiverType: Type? = null
        private set

    override val isSuspend = false // TODO: it should probably be true sometimes, but it never was

    override fun generateLambdaBody(sourceCompiler: SourceCompilerForInline, reifiedTypeInliner: ReifiedTypeInliner<*>) {
        val classBytes = loadClassBytesByInternalName(sourceCompiler.state, lambdaClassType.internalName)
        val superName = ClassReader(classBytes).superName
        val isPropertyReference = superName in PROPERTY_REFERENCE_SUPER_CLASSES
        val isFunctionReference = superName == FUNCTION_REFERENCE.internalName || superName == FUNCTION_REFERENCE_IMPL.internalName

        val descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, *capturedArgs)
        val constructor = getMethodNode(classBytes, "<init>", descriptor, lambdaClassType)?.node
        assert(constructor != null || capturedArgs.isEmpty()) {
            "Can't find non-default constructor <init>$descriptor for default lambda $lambdaClassType"
        }
        capturedVars =
            if (isFunctionReference || isPropertyReference)
                constructor?.desc?.let { Type.getArgumentTypes(it) }?.singleOrNull()?.let {
                    originalBoundReceiverType = it
                    listOf(capturedParamDesc(AsmUtil.RECEIVER_PARAMETER_NAME, AsmUtil.boxType(it)))
                } ?: emptyList()
            else
                constructor?.findCapturedFieldAssignmentInstructions()?.map { fieldNode ->
                    capturedParamDesc(fieldNode.name, Type.getType(fieldNode.desc))
                }?.toList() ?: emptyList()
        isBoundCallableReference = (isFunctionReference || isPropertyReference) && capturedVars.isNotEmpty()

        invokeMethodDescriptor = findInvokeMethodDescriptor(isPropertyReference)
        val methodName = (if (isPropertyReference) OperatorNameConventions.GET else OperatorNameConventions.INVOKE).asString()
        val signature = mapAsmSignature(sourceCompiler, invokeMethodDescriptor)
        node = getMethodNode(classBytes, methodName, signature.descriptor, lambdaClassType, signatureAmbiguity = true)
            ?: error("Can't find method '$signature' in '${lambdaClassType.internalName}'")
        invokeMethod = Method(node.node.name, node.node.desc)
        if (needReification) {
            //nested classes could also require reification
            reifiedTypeParametersUsages.mergeAll(reifiedTypeInliner.reifyInstructions(node.node))
        }
    }

    protected abstract fun mapAsmSignature(sourceCompiler: SourceCompilerForInline, descriptor: FunctionDescriptor): Method

    // TODO: get rid of this; descriptors should *only* be used by PsiDefaultLambda
    protected abstract fun findInvokeMethodDescriptor(isPropertyReference: Boolean): FunctionDescriptor

    private companion object {
        val PROPERTY_REFERENCE_SUPER_CLASSES =
            listOf(
                PROPERTY_REFERENCE0, PROPERTY_REFERENCE1, PROPERTY_REFERENCE2,
                MUTABLE_PROPERTY_REFERENCE0, MUTABLE_PROPERTY_REFERENCE1, MUTABLE_PROPERTY_REFERENCE2
            ).plus(OPTIMIZED_PROPERTY_REFERENCE_SUPERTYPES)
                .mapTo(HashSet(), Type::getInternalName)
    }
}
