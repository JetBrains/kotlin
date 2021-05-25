/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.codegen.BaseExpressionCodegen
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
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

    val lazySourceMapper: SourceMapper

    fun generateLambdaBody(lambdaInfo: ExpressionLambda, reifiedTypeParameters: ReifiedTypeParametersUsages): SMAPAndMethodNode

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

    fun generateFinallyBlocksIfNeeded(codegen: BaseExpressionCodegen, returnType: Type, afterReturnLabel: Label, target: Label?)

    fun isCallInsideSameModuleAsDeclared(functionDescriptor: FunctionDescriptor): Boolean

    fun isFinallyMarkerRequired(): Boolean

    val compilationContextDescriptor: DeclarationDescriptor

    val compilationContextFunctionDescriptor: FunctionDescriptor

    fun getContextLabels(): Map<String, Label?>

    fun reportSuspensionPointInsideMonitor(stackTraceElement: String)
}
