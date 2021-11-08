/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.File

class InlineCallSiteInfo(
    val ownerClassName: String,
    val method: Method,
    val inlineScopeVisibility: DescriptorVisibility?,
    val file: File?,
    val lineNumber: Int
) {
    val isInlineOrInsideInline: Boolean
        get() = inlineScopeVisibility != null

    val isInPublicInlineScope: Boolean
        get() = inlineScopeVisibility != null && !DescriptorVisibilities.isPrivate(inlineScopeVisibility)
}

interface SourceCompilerForInline {
    val state: GenerationState

    val callElement: Any

    val callElementText: String

    val inlineCallSiteInfo: InlineCallSiteInfo

    val sourceMapper: SourceMapper

    fun generateLambdaBody(lambdaInfo: ExpressionLambda, reifiedTypeParameters: ReifiedTypeParametersUsages): SMAPAndMethodNode

    fun compileInlineFunction(jvmSignature: JvmMethodSignature): SMAPAndMethodNode

    fun hasFinallyBlocks(): Boolean

    fun generateFinallyBlocks(finallyNode: MethodNode, curFinallyDepth: Int, returnType: Type, afterReturnLabel: Label, target: Label?)

    val isCallInsideSameModuleAsCallee: Boolean

    val isFinallyMarkerRequired: Boolean

    fun isSuspendLambdaCapturedByOuterObjectOrLambda(name: String): Boolean

    fun getContextLabels(): Map<String, Label?>

    fun reportSuspensionPointInsideMonitor(stackTraceElement: String)
}

fun GenerationState.trackLookup(container: FqName, functionName: String, location: LocationInfo) {
    val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return
    synchronized(lookupTracker) {
        lookupTracker.record(
            location.filePath,
            if (lookupTracker.requiresPosition) location.position else Position.NO_POSITION,
            container.asString(),
            ScopeKind.CLASSIFIER,
            functionName
        )
    }
}

fun loadCompiledInlineFunction(
    containerId: ClassId,
    asmMethod: Method,
    isSuspend: Boolean,
    isMangled: Boolean,
    state: GenerationState
): SMAPAndMethodNode {
    val containerType = AsmUtil.asmTypeByClassId(containerId)
    val resultInCache = state.inlineCache.methodNodeById.getOrPut(MethodId(containerType.descriptor, asmMethod)) {
        val bytes = state.inlineCache.classBytes.getOrPut(containerType.internalName) {
            findVirtualFile(state, containerId)?.contentsToByteArray()
                ?: throw IllegalStateException("Couldn't find declaration file for $containerId")
        }
        getMethodNode(containerType, bytes, asmMethod, isSuspend, isMangled)
    }
    return SMAPAndMethodNode(cloneMethodNode(resultInCache.node), resultInCache.classSMAP)
}

private fun getMethodNode(
    owner: Type,
    bytes: ByteArray,
    method: Method,
    isSuspend: Boolean,
    isMangled: Boolean
): SMAPAndMethodNode {
    getMethodNode(owner, bytes, method, isSuspend)?.let { return it }
    if (isMangled) {
        // Compatibility with old inline class ABI versions.
        val dashIndex = method.name.indexOf('-')
        val nameWithoutManglingSuffix = if (dashIndex > 0) method.name.substring(0, dashIndex) else method.name
        if (nameWithoutManglingSuffix != method.name) {
            getMethodNode(owner, bytes, Method(nameWithoutManglingSuffix, method.descriptor), isSuspend)?.let { return it }
        }
        getMethodNode(owner, bytes, Method("$nameWithoutManglingSuffix-impl", method.descriptor), isSuspend)?.let { return it }
    }
    throw IllegalStateException("couldn't find inline method $owner.$method")
}

// If an `inline suspend fun` has a state machine, it should have a `$$forInline` version without one.
private fun getMethodNode(owner: Type, bytes: ByteArray, method: Method, isSuspend: Boolean) =
    (if (isSuspend) getMethodNode(bytes, owner, Method(method.name + FOR_INLINE_SUFFIX, method.descriptor)) else null)
        ?: getMethodNode(bytes, owner, method)
