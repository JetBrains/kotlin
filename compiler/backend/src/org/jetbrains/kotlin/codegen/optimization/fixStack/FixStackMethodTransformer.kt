/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.optimization.fixStack

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.MethodAnalyzer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.fixStack.forEachPseudoInsn
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.codegen.pseudoInsns.parsePseudoInsnOrNull
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import java.util.*
import kotlin.properties.Delegates

public object FixStackMethodTransformer : MethodTransformer() {
    public override fun transform(internalClassName: String, methodNode: MethodNode) {
        val context = FixStackContext(methodNode)

        if (!context.hasAnyMarkers()) return

        // If inline method markers are inconsistent, remove them now
        if (!context.consistentInlineMarkers) {
            InsnSequence(methodNode.instructions).forEach { insnNode ->
                if (InlineCodegenUtil.isInlineMarker(insnNode))
                    methodNode.instructions.remove(insnNode)
            }
        }

        if (context.isAnalysisRequired()) {
            val analyzer = FixStackAnalyzer(internalClassName, methodNode, context)
            analyzer.analyze()

            methodNode.maxStack = methodNode.maxStack + analyzer.maxExtraStackSize

            val actions = arrayListOf<() -> Unit>()

            transformBreakContinueGotos(methodNode, context, actions, analyzer)

            transformSaveRestoreStackMarkers(methodNode, context, actions, analyzer)

            actions.forEach { it() }
        }

        context.fakeAlwaysTrueIfeqMarkers.forEach { marker ->
            replaceAlwaysTrueIfeqWithGoto(methodNode, marker)
        }

        context.fakeAlwaysFalseIfeqMarkers.forEach { marker ->
            removeAlwaysFalseIfeq(methodNode, marker)
        }
    }

    private fun transformBreakContinueGotos(
            methodNode: MethodNode,
            fixStackContext: FixStackContext,
            actions: MutableList<() -> Unit>,
            analyzer: FixStackAnalyzer
    ) {
        fixStackContext.breakContinueGotoNodes.forEach { gotoNode ->
            val gotoIndex = methodNode.instructions.indexOf(gotoNode)
            val labelIndex = methodNode.instructions.indexOf(gotoNode.label)

            val DEAD_CODE = -1 // Stack size is always non-negative
            val actualStackSize = analyzer.frames[gotoIndex]?.getStackSize() ?: DEAD_CODE
            val expectedStackSize = analyzer.frames[labelIndex]?.getStackSize() ?: DEAD_CODE

            if (actualStackSize != DEAD_CODE && expectedStackSize != DEAD_CODE) {
                assert(expectedStackSize <= actualStackSize,
                       "Label at $labelIndex, jump at $gotoIndex: stack underflow: $expectedStackSize > $actualStackSize")
                val frame = analyzer.frames[gotoIndex]!!
                actions.add({ replaceMarkerWithPops(methodNode, gotoNode.getPrevious(), expectedStackSize, frame) })
            }
            else if (actualStackSize != DEAD_CODE && expectedStackSize == DEAD_CODE) {
                throw AssertionError("Live jump $gotoIndex to dead label $labelIndex")
            }
            else {
                val marker = gotoNode.getPrevious()
                actions.add({ methodNode.instructions.remove(marker) })
            }
        }
    }

    private fun transformSaveRestoreStackMarkers(
            methodNode: MethodNode,
            context: FixStackContext,
            actions: MutableList<() -> Unit>,
            analyzer: FixStackAnalyzer
    ) {
        val localVariablesManager = LocalVariablesManager(context, methodNode)
        InsnSequence(methodNode.instructions).forEach { marker ->
            val pseudoInsn = parsePseudoInsnOrNull(marker)
            when {
                pseudoInsn == PseudoInsn.SAVE_STACK_BEFORE_TRY ->
                    transformSaveStackMarker(methodNode, actions, analyzer, marker, localVariablesManager)
                pseudoInsn == PseudoInsn.RESTORE_STACK_IN_TRY_CATCH ->
                    transformRestoreStackMarker(methodNode, actions, marker, localVariablesManager)
                InlineCodegenUtil.isBeforeInlineMarker(marker) ->
                    transformBeforeInlineCallMarker(methodNode, actions, analyzer, marker, localVariablesManager)
                InlineCodegenUtil.isAfterInlineMarker(marker) ->
                    transformAfterInlineCallMarker(methodNode, actions, analyzer, marker, localVariablesManager)
            }
        }
    }

    private fun transformSaveStackMarker(
            methodNode: MethodNode,
            actions: MutableList<() -> Unit>,
            analyzer: FixStackAnalyzer,
            marker: AbstractInsnNode,
            localVariablesManager: LocalVariablesManager
    ) {
        val savedStackValues = analyzer.savedStacks[marker]
        if (savedStackValues != null) {
            val savedStackDescriptor = localVariablesManager.allocateVariablesForSaveStackMarker(marker, savedStackValues)
            actions.add({ saveStack(methodNode, marker, savedStackDescriptor, false) })
        }
        else {
            // marker is dead code
            localVariablesManager.allocateVariablesForSaveStackMarker(marker, emptyList())
            actions.add({ methodNode.instructions.remove(marker) })
        }
    }

    private fun transformRestoreStackMarker(
            methodNode: MethodNode,
            actions: MutableList<() -> Unit>,
            marker: AbstractInsnNode,
            localVariablesManager: LocalVariablesManager
    ) {
        val savedStackDescriptor = localVariablesManager.getSavedStackDescriptorOrNull(marker)
        if (savedStackDescriptor != null) {
            actions.add({ restoreStack(methodNode, marker, savedStackDescriptor) })
        }
        else {
            // marker is dead code
            actions.add({ methodNode.instructions.remove(marker) })
        }
        localVariablesManager.markRestoreStackMarkerEmitted(marker)
    }

    private fun transformAfterInlineCallMarker(
            methodNode: MethodNode,
            actions: MutableList<() -> Unit>,
            analyzer: FixStackAnalyzer,
            inlineMarker: AbstractInsnNode,
            localVariablesManager: LocalVariablesManager
    ) {
        val savedStackDescriptor = localVariablesManager.getBeforeInlineDescriptor(inlineMarker)
        val afterInlineFrame = analyzer.getFrame(inlineMarker) as FixStackAnalyzer.FixStackFrame?
        if (afterInlineFrame != null && savedStackDescriptor.isNotEmpty()) {
            assert(afterInlineFrame.getStackSize() <= 1, "Inline method should not leave more than 1 value on stack")
            if (afterInlineFrame.getStackSize() == 1) {
                val afterInlineStackValues = afterInlineFrame.getStackContent()
                val returnValue = afterInlineStackValues.last()
                val returnValueLocalVarIndex = localVariablesManager.createReturnValueVariable(returnValue)
                actions.add({
                                restoreStackWithReturnValue(methodNode, inlineMarker, savedStackDescriptor,
                                                            returnValue, returnValueLocalVarIndex)
                            })
            }
            else {
                actions.add({ restoreStack(methodNode, inlineMarker, savedStackDescriptor) })
            }
        }
        else {
            // after inline marker is dead code
            actions.add({ methodNode.instructions.remove(inlineMarker) })
        }
        localVariablesManager.markAfterInlineMarkerEmitted(inlineMarker)
    }

    private fun transformBeforeInlineCallMarker(
            methodNode: MethodNode,
            actions: MutableList<() -> Unit>,
            analyzer: FixStackAnalyzer,
            inlineMarker: AbstractInsnNode,
            localVariablesManager: LocalVariablesManager
    ) {
        val savedStackValues = analyzer.savedStacks[inlineMarker]
        if (savedStackValues != null) {
            val savedStackDescriptor = localVariablesManager.allocateVariablesForBeforeInlineMarker(inlineMarker, savedStackValues)
            actions.add({ saveStack(methodNode, inlineMarker, savedStackDescriptor, false) })
        }
        else {
            // before inline marker is dead code
            localVariablesManager.allocateVariablesForBeforeInlineMarker(inlineMarker, emptyList())
            actions.add({ methodNode.instructions.remove(inlineMarker) })
        }
    }


}