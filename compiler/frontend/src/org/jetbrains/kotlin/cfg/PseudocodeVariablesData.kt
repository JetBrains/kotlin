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

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.ReadValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils.variableDescriptorForDeclaration
import java.util.*

class PseudocodeVariablesData(val pseudocode: Pseudocode, private val bindingContext: BindingContext) {
    private val pseudocodeVariableDataCollector = PseudocodeVariableDataCollector(bindingContext, pseudocode)

    private val declaredVariablesForDeclaration = hashMapOf<Pseudocode, Set<VariableDescriptor>>()

    val variableInitializers: Map<Instruction, Edges<InitControlFlowInfo>> by lazy {
        computeVariableInitializers()
    }

    val blockScopeVariableInfo: BlockScopeVariableInfo
        get() = pseudocodeVariableDataCollector.blockScopeVariableInfo

    fun getDeclaredVariables(pseudocode: Pseudocode, includeInsideLocalDeclarations: Boolean): Set<VariableDescriptor> {
        if (!includeInsideLocalDeclarations) {
            return getUpperLevelDeclaredVariables(pseudocode)
        }
        val declaredVariables = linkedSetOf<VariableDescriptor>()
        declaredVariables.addAll(getUpperLevelDeclaredVariables(pseudocode))

        for (localFunctionDeclarationInstruction in pseudocode.localDeclarations) {
            val localPseudocode = localFunctionDeclarationInstruction.body
            declaredVariables.addAll(getUpperLevelDeclaredVariables(localPseudocode))
        }
        return declaredVariables
    }

    private fun getUpperLevelDeclaredVariables(pseudocode: Pseudocode): Set<VariableDescriptor> {
        var declaredVariables = declaredVariablesForDeclaration[pseudocode]
        if (declaredVariables == null) {
            declaredVariables = computeDeclaredVariablesForPseudocode(pseudocode)
            declaredVariablesForDeclaration.put(pseudocode, declaredVariables)
        }
        return declaredVariables
    }

    private fun computeDeclaredVariablesForPseudocode(pseudocode: Pseudocode): Set<VariableDescriptor> {
        val declaredVariables = linkedSetOf<VariableDescriptor>()
        for (instruction in pseudocode.instructions) {
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                variableDescriptorForDeclaration(descriptor)?.let {
                    declaredVariables.add(it)
                }
            }
        }
        return Collections.unmodifiableSet(declaredVariables)
    }

    // variable initializers

    private fun computeVariableInitializers(): Map<Instruction, Edges<InitControlFlowInfo>> {

        val blockScopeVariableInfo = pseudocodeVariableDataCollector.blockScopeVariableInfo

        return pseudocodeVariableDataCollector.collectData(TraversalOrder.FORWARD, InitControlFlowInfo()) {
            instruction: Instruction, incomingEdgesData: Collection<InitControlFlowInfo> ->

            val enterInstructionData = mergeIncomingEdgesDataForInitializers(instruction, incomingEdgesData, blockScopeVariableInfo)
            val exitInstructionData = addVariableInitStateFromCurrentInstructionIfAny(
                    instruction, enterInstructionData, blockScopeVariableInfo)
            Edges(enterInstructionData, exitInstructionData)
        }
    }

    private fun addVariableInitStateFromCurrentInstructionIfAny(
            instruction: Instruction,
            enterInstructionData: InitControlFlowInfo,
            blockScopeVariableInfo: BlockScopeVariableInfo): InitControlFlowInfo {
        if (instruction is MagicInstruction) {
            if (instruction.kind === MagicKind.EXHAUSTIVE_WHEN_ELSE) {
                return enterInstructionData.iterator().fold(enterInstructionData) {
                    result, (key, value) ->
                    if (!value.definitelyInitialized()) {
                        result.put(key, VariableControlFlowState.createInitializedExhaustively(value.isDeclared))
                    }
                    else result
                }
            }
        }
        if (instruction !is WriteValueInstruction && instruction !is VariableDeclarationInstruction) {
            return enterInstructionData
        }
        val variable = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext) ?: return enterInstructionData
        var exitInstructionData = enterInstructionData
        if (instruction is WriteValueInstruction) {
            // if writing to already initialized object
            if (!PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, bindingContext)) {
                return enterInstructionData
            }

            val enterInitState = enterInstructionData.getOrNull(variable)
            val initializationAtThisElement = VariableControlFlowState.create(instruction.element is KtProperty, enterInitState)
            exitInstructionData = exitInstructionData.put(variable, initializationAtThisElement, enterInitState)
        }
        else {
            // instruction instanceof VariableDeclarationInstruction
            var enterInitState: VariableControlFlowState? = enterInstructionData.getOrNull(variable)
            if (enterInitState == null) {
                enterInitState = getDefaultValueForInitializers(variable, instruction, blockScopeVariableInfo)
            }
            if (!enterInitState.mayBeInitialized() || !enterInitState.isDeclared) {
                val variableDeclarationInfo = VariableControlFlowState.create(enterInitState.initState, isDeclared = true)
                exitInstructionData = exitInstructionData.put(variable, variableDeclarationInfo, enterInitState)
            }
        }
        return exitInstructionData
    }

    // variable use

    val variableUseStatusData: Map<Instruction, Edges<UseControlFlowInfo>>
        get() = pseudocodeVariableDataCollector.collectData(TraversalOrder.BACKWARD, UseControlFlowInfo()) {
            instruction: Instruction, incomingEdgesData: Collection<UseControlFlowInfo> ->

            val enterResult: UseControlFlowInfo = if (incomingEdgesData.size == 1) {
                incomingEdgesData.single()
            }
            else {
                incomingEdgesData.fold(UseControlFlowInfo()) { result, edgeData ->
                    edgeData.iterator().fold(result) { subResult, (variableDescriptor, variableUseState) ->
                        subResult.put(variableDescriptor, variableUseState.merge(subResult.getOrNull(variableDescriptor)))
                    }
                }
            }

            val variableDescriptor = PseudocodeUtil.extractVariableDescriptorFromReference(instruction, bindingContext)
            if (variableDescriptor == null || instruction !is ReadValueInstruction && instruction !is WriteValueInstruction) {
                Edges(enterResult, enterResult)
            }
            else {
                val exitResult =
                    if (instruction is ReadValueInstruction) {
                        enterResult.put(variableDescriptor, VariableUseState.READ)
                    }
                    else {
                        var variableUseState: VariableUseState? = enterResult.getOrNull(variableDescriptor)
                        if (variableUseState == null) {
                            variableUseState = VariableUseState.UNUSED
                        }
                        when (variableUseState) {
                            VariableUseState.UNUSED, VariableUseState.ONLY_WRITTEN_NEVER_READ ->
                                enterResult.put(variableDescriptor, VariableUseState.ONLY_WRITTEN_NEVER_READ)
                            VariableUseState.WRITTEN_AFTER_READ, VariableUseState.READ ->
                                enterResult.put(variableDescriptor, VariableUseState.WRITTEN_AFTER_READ)
                        }
                    }
                Edges(enterResult, exitResult)
            }
        }

    companion object {

        @JvmStatic
        fun getDefaultValueForInitializers(
                variable: VariableDescriptor,
                instruction: Instruction,
                blockScopeVariableInfo: BlockScopeVariableInfo
        ): VariableControlFlowState {
            //todo: think of replacing it with "MapWithDefaultValue"
            val declaredIn = blockScopeVariableInfo.declaredIn[variable]
            val declaredOutsideThisDeclaration =
                    declaredIn == null //declared outside this pseudocode
                    || declaredIn.blockScopeForContainingDeclaration != instruction.blockScope.blockScopeForContainingDeclaration
            return VariableControlFlowState.create(isInitialized = declaredOutsideThisDeclaration)
        }

        private val EMPTY_INIT_CONTROL_FLOW_INFO = InitControlFlowInfo()

        private fun mergeIncomingEdgesDataForInitializers(
                instruction: Instruction,
                incomingEdgesData: Collection<InitControlFlowInfo>,
                blockScopeVariableInfo: BlockScopeVariableInfo
        ): InitControlFlowInfo {
            if (incomingEdgesData.size == 1) return incomingEdgesData.single()
            if (incomingEdgesData.isEmpty()) return EMPTY_INIT_CONTROL_FLOW_INFO
            val variablesInScope = linkedSetOf<VariableDescriptor>()
            for (edgeData in incomingEdgesData) {
                variablesInScope.addAll(edgeData.keySet())
            }

            return variablesInScope.fold(EMPTY_INIT_CONTROL_FLOW_INFO) { result, variable ->
                var initState: InitState? = null
                var isDeclared = true
                for (edgeData in incomingEdgesData) {
                    val varControlFlowState = edgeData.getOrNull(variable)
                                              ?: getDefaultValueForInitializers(variable, instruction, blockScopeVariableInfo)
                    initState = initState?.merge(varControlFlowState.initState) ?: varControlFlowState.initState
                    if (!varControlFlowState.isDeclared) {
                        isDeclared = false
                    }
                }
                if (initState == null) {
                    throw AssertionError("An empty set of incoming edges data")
                }
                result.put(variable, VariableControlFlowState.create(initState, isDeclared))
            }
        }
    }
}
