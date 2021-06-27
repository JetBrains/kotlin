/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode

interface FunctionalArgument

abstract class LambdaInfo : FunctionalArgument {
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

    open val hasDispatchReceiver = true

    fun addAllParameters(remapper: FieldRemapper): Parameters {
        val builder = ParametersBuilder.initializeBuilderFrom(OBJECT_TYPE, invokeMethod.descriptor, this)

        for (info in capturedVars) {
            val field = remapper.findField(FieldInsnNode(0, info.containingLambdaName, info.fieldName, ""))
                ?: error("Captured field not found: " + info.containingLambdaName + "." + info.fieldName)
            val recapturedParamInfo = builder.addCapturedParam(field, info.fieldName)
            if (info.isSuspend) {
                recapturedParamInfo.functionalArgument = NonInlineableArgumentForInlineableParameterCalledInSuspend
            }
        }

        return builder.buildParameters()
    }

    companion object {
        fun LambdaInfo.capturedParamDesc(fieldName: String, fieldType: Type, isSuspend: Boolean): CapturedParamDesc {
            return CapturedParamDesc(lambdaClassType, fieldName, fieldType, isSuspend)
        }
    }
}

object NonInlineableArgumentForInlineableParameterCalledInSuspend : FunctionalArgument
object NonInlineableArgumentForInlineableSuspendParameter : FunctionalArgument

abstract class ExpressionLambda : LambdaInfo() {
    fun generateLambdaBody(sourceCompiler: SourceCompilerForInline) {
        node = sourceCompiler.generateLambdaBody(this, reifiedTypeParametersUsages)
        node.node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
    }
}

abstract class DefaultLambda(
    final override val lambdaClassType: Type,
    capturedArgs: Array<Type>,
    val offset: Int,
    val needReification: Boolean,
    sourceCompiler: SourceCompilerForInline
) : LambdaInfo() {
    final override val isSuspend
        get() = false // TODO: it should probably be true sometimes, but it never was
    final override val isBoundCallableReference: Boolean
    final override val capturedVars: List<CapturedParamDesc>

    final override val invokeMethod: Method
        get() = Method(node.node.name, node.node.desc)

    val originalBoundReceiverType: Type?

    protected val isPropertyReference: Boolean
    protected val isFunctionReference: Boolean

    init {
        val classBytes = loadClass(sourceCompiler)
        val superName = ClassReader(classBytes).superName
        isPropertyReference = superName in PROPERTY_REFERENCE_SUPER_CLASSES
        isFunctionReference = superName == FUNCTION_REFERENCE.internalName || superName == FUNCTION_REFERENCE_IMPL.internalName

        val constructorMethod = Method("<init>", Type.VOID_TYPE, capturedArgs)
        val constructor = getMethodNode(classBytes, lambdaClassType, constructorMethod)?.node
        assert(constructor != null || capturedArgs.isEmpty()) {
            "can't find constructor '$constructorMethod' for default lambda '${lambdaClassType.internalName}'"
        }
        // This only works for primitives but not inline classes, since information about the Kotlin type of the bound
        // receiver is not present anywhere. This is why with JVM_IR the constructor argument of bound references
        // is already `Object`, and this field is never used.
        originalBoundReceiverType =
            capturedArgs.singleOrNull()?.takeIf { (isFunctionReference || isPropertyReference) && AsmUtil.isPrimitive(it) }
        capturedVars =
            if (isFunctionReference || isPropertyReference)
                capturedArgs.singleOrNull()?.let {
                    listOf(capturedParamDesc(AsmUtil.RECEIVER_PARAMETER_NAME, AsmUtil.boxType(it), isSuspend = false))
                } ?: emptyList()
            else
                constructor?.findCapturedFieldAssignmentInstructions()?.map { fieldNode ->
                    capturedParamDesc(fieldNode.name, Type.getType(fieldNode.desc), isSuspend = false)
                }?.toList() ?: emptyList()
        isBoundCallableReference = (isFunctionReference || isPropertyReference) && capturedVars.isNotEmpty()
    }

    private fun loadClass(sourceCompiler: SourceCompilerForInline): ByteArray =
        sourceCompiler.state.inlineCache.classBytes.getOrPut(lambdaClassType.internalName) {
            loadClassBytesByInternalName(sourceCompiler.state, lambdaClassType.internalName)
        }

    // Returns whether the loaded invoke is erased, i.e. the name equals the fallback and all types are `Object`.
    protected fun loadInvoke(sourceCompiler: SourceCompilerForInline, erasedName: String, actualMethod: Method): Boolean {
        node = getMethodNodeImprecise(loadClass(sourceCompiler), lambdaClassType, actualMethod, erasedName)
            ?: error("Can't find method '$actualMethod' in '${lambdaClassType.internalName}'")
        return invokeMethod.run { name == erasedName && returnType == OBJECT_TYPE && argumentTypes.all { it == OBJECT_TYPE } }
    }

    private companion object {
        fun getMethodNodeImprecise(classBytes: ByteArray, classType: Type, method: Method, erasedName: String) =
            getMethodNode(classBytes, classType) { it, access ->
                (it.name == method.name || it.name == erasedName) &&
                        it.argumentTypes.size == method.argumentTypes.size && access.and(Opcodes.ACC_SYNTHETIC) == 0
            }

        val PROPERTY_REFERENCE_SUPER_CLASSES =
            listOf(
                PROPERTY_REFERENCE0, PROPERTY_REFERENCE1, PROPERTY_REFERENCE2,
                MUTABLE_PROPERTY_REFERENCE0, MUTABLE_PROPERTY_REFERENCE1, MUTABLE_PROPERTY_REFERENCE2
            ).plus(OPTIMIZED_PROPERTY_REFERENCE_SUPERTYPES)
                .mapTo(HashSet(), Type::getInternalName)
    }
}
