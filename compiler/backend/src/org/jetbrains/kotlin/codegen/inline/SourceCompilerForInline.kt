/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

interface SourceCompilerForInline {
    val state: GenerationState

    val callElement: Any

    val lookupLocation: LookupLocation

    val callElementText: String

    val callsiteFile: PsiFile?

    val contextKind: OwnerKind

    val inlineCallSiteInfo: InlineCallSiteInfo

    val sourceMapper: SourceMapper

    fun generateLambdaBody(lambdaInfo: ExpressionLambda, reifiedTypeParameters: ReifiedTypeParametersUsages): SMAPAndMethodNode

    fun compileInlineFunction(jvmSignature: JvmMethodSignature): SMAPAndMethodNode

    fun hasFinallyBlocks(): Boolean

    fun createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(
        finallyNode: MethodNode,
        curFinallyDepth: Int
    ): BaseExpressionCodegen

    fun generateFinallyBlocksIfNeeded(codegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label, target: Label?)

    val isCallInsideSameModuleAsCallee: Boolean

    val isFinallyMarkerRequired: Boolean

    val compilationContextDescriptor: DeclarationDescriptor

    val compilationContextFunctionDescriptor: FunctionDescriptor

    fun getContextLabels(): Map<String, Label?>

    fun reportSuspensionPointInsideMonitor(stackTraceElement: String)
}

fun loadCompiledInlineFunction(
    containerId: ClassId,
    asmMethod: Method,
    isSuspend: Boolean,
    isMangled: Boolean,
    state: GenerationState
): SMAPAndMethodNode {
    val containerType = AsmUtil.asmTypeByClassId(containerId)
    val bytes = state.inlineCache.classBytes.getOrPut(containerId) {
        findVirtualFile(state, containerId)?.contentsToByteArray()
            ?: throw IllegalStateException("Couldn't find declaration file for $containerId")
    }
    val resultInCache = state.inlineCache.methodNodeById.getOrPut(MethodId(containerType.descriptor, asmMethod)) {
        getMethodNode(containerType, bytes, asmMethod.name, asmMethod.descriptor, isSuspend, isMangled)
    }
    return SMAPAndMethodNode(cloneMethodNode(resultInCache.node), resultInCache.classSMAP)
}

private fun getMethodNode(
    owner: Type,
    bytes: ByteArray,
    name: String,
    descriptor: String,
    isSuspend: Boolean,
    isMangled: Boolean
): SMAPAndMethodNode {
    getMethodNode(owner, bytes, name, descriptor, isSuspend)?.let { return it }
    if (isMangled) {
        // Compatibility with old inline class ABI versions.
        val dashIndex = name.indexOf('-')
        val nameWithoutManglingSuffix = if (dashIndex > 0) name.substring(0, dashIndex) else name
        if (nameWithoutManglingSuffix != name) {
            getMethodNode(owner, bytes, nameWithoutManglingSuffix, descriptor, isSuspend)?.let { return it }
        }
        getMethodNode(owner, bytes, "$nameWithoutManglingSuffix-impl", descriptor, isSuspend)?.let { return it }
    }
    throw IllegalStateException("couldn't find inline method $owner.$name$descriptor")
}

// If an `inline suspend fun` has a state machine, it should have a `$$forInline` version without one.
private fun getMethodNode(owner: Type, bytes: ByteArray, name: String, descriptor: String, isSuspend: Boolean) =
    (if (isSuspend) getMethodNode(bytes, name + FOR_INLINE_SUFFIX, descriptor, owner) else null)
        ?: getMethodNode(bytes, name, descriptor, owner)
