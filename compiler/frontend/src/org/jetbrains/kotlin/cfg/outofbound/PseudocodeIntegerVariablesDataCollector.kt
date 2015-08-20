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

package org.jetbrains.kotlin.cfg.outofbound

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.JetNodeType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.LexicalScopeVariableInfo
import org.jetbrains.kotlin.cfg.LexicalScopeVariableInfoImpl
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.NondeterministicJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.UnconditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.collectData
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.HashMap
import java.util.HashSet

// This file contains functionality similar to org.jetbrains.kotlin.cfg.PseudocodeVariablesData,
// but collects information about integer variables' values. Semantically it would be better to
// merge functionality in this two files

public class PseudocodeIntegerVariablesDataCollector(val pseudocode: Pseudocode, val bindingContext: BindingContext) {
    private val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)
    private val loopsInfo: LoopsInfo = createLoopsInfoMap()

    // this function is fully copied from PseudocodeVariableDataCollector
    private fun computeLexicalScopeVariableInfo(pseudocode: Pseudocode): LexicalScopeVariableInfo {
        val lexicalScopeVariableInfo = LexicalScopeVariableInfoImpl()
        pseudocode.traverse(TraversalOrder.FORWARD, { instruction ->
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                if (descriptor != null) {
                    // TODO: investigate why tests fail without this eager computation here
                    descriptor.toString()

                    assert(descriptor is VariableDescriptor) {
                        "Variable descriptor should correspond to the instruction for ${instruction.element.getText()}.\n" +
                        "Descriptor: $descriptor"
                    }
                    lexicalScopeVariableInfo.registerVariableDeclaredInScope(
                            descriptor as VariableDescriptor, instruction.lexicalScope
                    )
                }
            }
        })
        return lexicalScopeVariableInfo
    }

    // Control flow graph's oriented edge
    private data class OrientedEdge (val from: Instruction, val to: Instruction)
    // Contains additional info about loops required for processing
    private data class LoopsInfo(
            // Contains the following mapping: (loop enter instruction) to (set of variables updated inside loop)
            val updatedVariables: HashMap<Instruction, Set<VariableDescriptor>>,
            // Contains loops' back edges, i.e. the edges that lead to loop enter from loop's body
            // (such edges create cycles in control flow graph)
            val backEdges: HashSet<OrientedEdge>
    )

    private fun createLoopsInfoMap(): LoopsInfo {
        if (pseudocode !is PseudocodeImpl) {
            throw IllegalArgumentException("Pseudocode is not a PseudocodeImpl so there is no labels info")
        }
        val labels = pseudocode.labels
        val instructionIndexToLabels = HashMap<Int, MutableSet<PseudocodeImpl.PseudocodeLabel>>(labels.size() + 5, 1f) // + 5 is chosen randomly
        labels.forEach { instructionIndexToLabels.getOrPut(it.targetInstructionIndex, { HashSet() }).add(it) }
        val updatedVariablesMap = HashMap<Instruction, Set<VariableDescriptor>>(labels.size(), 1f)
        val variablesStack = linkedListOf<Pair<Instruction, HashSet<VariableDescriptor>>>()
        val loopEntryPointText = "loop entry point"
        val loopExitPointText = "loop exit point"
        val conditionEntryPointText = "condition entry point"
        fun extractUpdatedVariablesInfoIfAny(index: Int, instruction: Instruction) {
            val instructionLabels = instructionIndexToLabels[index]
            if (instructionLabels != null) {
                val labelsAsStrings = instructionLabels.map { it.toString() }
                if (labelsAsStrings.any { it contains loopEntryPointText }) {
                    variablesStack.push(instruction to hashSetOf())
                }
                else if (labelsAsStrings.any { it contains loopExitPointText }) {
                    val topInfo = variablesStack.removeFirst()
                    updatedVariablesMap[topInfo.first] = topInfo.second
                }
            }
            if (instruction is ConditionalJumpInstruction &&
                (instruction.element is JetWhileExpression || instruction.element is JetDoWhileExpression)) {
                assert(variablesStack.isNotEmpty(), "Jump in while is met but there is still no loop info")
                val topInfo = variablesStack.removeFirst()
                variablesStack.push(instruction to topInfo.second)
            }
            if (!variablesStack.isEmpty()) {
                extractTargetDescriptorIfUpdateInstruction(instruction)?.let {
                    variablesStack.first().second.add(it)
                }
            }
        }
        val backEdgesSet = HashSet<OrientedEdge>(labels.size() * 2)
        fun extractBackEdgeInfoIfAny(instruction: Instruction) {
            if (instruction is ConditionalJumpInstruction &&
                instruction.element is JetDoWhileExpression &&
                instruction.targetLabel.toString() contains loopEntryPointText) {
                backEdgesSet.add(OrientedEdge(instruction, instruction.nextOnTrue))
            }
            if (instruction is UnconditionalJumpInstruction &&
                (instruction.element is JetForExpression ||
                 instruction.element is JetWhileExpression) &&
                instruction.targetLabel.toString() contains loopEntryPointText) {
                backEdgesSet.add(OrientedEdge(
                        instruction,
                        instruction.resolvedTarget ?: throw IllegalStateException("Jump instruction target in not resolved")
                ))
            }
            if (instruction is UnconditionalJumpInstruction && instruction.element is JetContinueExpression &&
                instruction.targetLabel.toString() contains conditionEntryPointText) {
                backEdgesSet.add(OrientedEdge(
                        instruction,
                        instruction.resolvedTarget ?: throw IllegalStateException("Jump instruction target in not resolved")
                ))
            }
        }
        pseudocode.instructionsIncludingDeadCode.forEachIndexed { index, instruction ->
            extractUpdatedVariablesInfoIfAny(index, instruction)
            extractBackEdgeInfoIfAny(instruction)
        }
        return LoopsInfo(updatedVariablesMap, backEdgesSet)
    }

    private fun extractTargetDescriptorIfUpdateInstruction(instruction: Instruction): VariableDescriptor? =
            when (instruction) {
                is WriteValueInstruction -> PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                is CallInstruction -> {
                    val pseudoAnnotation = CallInstructionUtils.tryExtractPseudoAnnotationForCollector(instruction)
                    when (pseudoAnnotation) {
                        is PseudoAnnotation.IncreaseSizeByConstantMethod,
                        is PseudoAnnotation.IncreaseSizeByPassedCollectionMethod,
                        is PseudoAnnotation.DecreaseSizeToZeroMethod,
                        is PseudoAnnotation.SetMethod -> tryExtractMethodCallReceiverDescriptor(instruction)
                        else -> null
                    }
                }
                else -> null
            }

    public fun collectVariableValuesData(): Map<Instruction, Edges<ValuesData>> {
        return pseudocode.collectData(
                TraversalOrder.FORWARD,
                /* mergeDataWithLocalDeclarations */ true,
                { instruction, incomingData: Collection<ValuesData> -> mergeVariablesValues(instruction, incomingData) },
                { previous, current, edgeData -> updateEdge(previous, current, edgeData) },
                ValuesData.Defined()
        )
    }

    private fun updateEdge(previousInstruction: Instruction, currentInstruction: Instruction, edgeData: ValuesData): ValuesData =
        if (edgeData is ValuesData.Defined) {
            if (loopsInfo.backEdges.contains(OrientedEdge(previousInstruction, currentInstruction))) {
                // The edge we need to update leads from loop's body to loop enter (for example, from while loop's body end
                // to while loop's condition). After the first traversal of all the instructions list, this edge will contain
                // the information that is computed after current instruction. In current implementation we don't process loop
                // bodies multiple times, so to avoid this computation we destroy the information on this edge.
                ValuesData.Defined()
            }
            else {
                val updatedEdgeData = edgeData.copy()
                val filteredEdgeData = removeOutOfScopeVariables(previousInstruction, currentInstruction, updatedEdgeData)
                applyRestrictionsOnValuesIfNeeded(previousInstruction, currentInstruction, filteredEdgeData)
            }
        }
        else edgeData.copy()

    private fun removeOutOfScopeVariables(
            previousInstruction: Instruction,
            currentInstruction: Instruction,
            edgeData: ValuesData.Defined
    ): ValuesData.Defined {
        val filteredIntVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.intVarsToValues)
        val filteredIntFakeVars = filterOutFakeVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.intFakeVarsToValues)
        val filteredBoolVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.boolVarsToValues)
        val filteredBoolFakeVars = filterOutFakeVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.boolFakeVarsToValues)
        val filteredArrayVars = filterOutVariablesOutOfScope(previousInstruction, currentInstruction, edgeData.collectionsToSizes)
        return ValuesData.Defined(
                HashMap(filteredIntVars),
                HashMap(filteredIntFakeVars),
                HashMap(filteredBoolVars),
                HashMap(filteredBoolFakeVars),
                HashMap(filteredArrayVars)
        )
    }

    // this function is fully copied from PseudocodeVariableDataCollector
    private fun <D> filterOutVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            data: Map<VariableDescriptor, D>
    ): Map<VariableDescriptor, D> {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.lexicalScope.depth
        if (toDepth >= from.lexicalScope.depth) return data

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        return data.filterKeys { variable ->
            val lexicalScope = lexicalScopeVariableInfo.declaredIn[variable]
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        }
    }

    // This method is much like the previous one. The generalization
    // must be done after merge with PseudocodeVariableDataCollector
    private fun <D> filterOutFakeVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            data: Map<PseudoValue, D>
    ): Map<PseudoValue, D> {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.lexicalScope.depth
        if (toDepth >= from.lexicalScope.depth) return data

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        return data.filterKeys { variable ->
            val lexicalScope = variable.createdAt?.lexicalScope
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        }
    }

    private fun applyRestrictionsOnValuesIfNeeded(
            previousInstruction: Instruction,
            currentInstruction: Instruction,
            edgeData: ValuesData.Defined
    ): ValuesData =
            if (previousInstruction is ConditionalJumpInstruction) {
                val conditionBoolValue = edgeData.boolFakeVarsToValues[previousInstruction.conditionValue]
                if (conditionBoolValue != null)
                    if (previousInstruction.element is JetIfExpression || previousInstruction.element is JetBinaryExpression)
                        applyRestrictionsOnValues(conditionBoolValue, currentInstruction, previousInstruction.nextOnTrue,
                                                  previousInstruction.nextOnFalse, edgeData)
                    else if (previousInstruction.element is JetDoWhileExpression || previousInstruction.element is JetWhileExpression)
                        applyRestrictionsOnValues(conditionBoolValue, currentInstruction, previousInstruction.nextOnTrue,
                                                  previousInstruction.nextOnFalse, edgeData, applyOnlyForThenBlock = true)
                    else edgeData
                else edgeData
            }
            else if (previousInstruction is NondeterministicJumpInstruction &&
                previousInstruction.element is JetWhenExpression &&
                previousInstruction.inputValue != null
            ) {
                val nextOnTrue = previousInstruction.next
                val nextOnFalse =
                        if (previousInstruction.resolvedTargets.size() > 0)
                            previousInstruction.resolvedTargets.values().first()
                        else throw IllegalStateException("Jump instruction target in `when` is not resolved")
                val conditionBoolValue = edgeData.boolFakeVarsToValues[previousInstruction.inputValue]
                if (conditionBoolValue != null)
                    applyRestrictionsOnValues(conditionBoolValue, currentInstruction, nextOnTrue, nextOnFalse, edgeData)
                else edgeData
            }
            else edgeData

    private fun applyRestrictionsOnValues(
            conditionBoolValue: BooleanVariableValue,
            currentInstruction: Instruction,
            onTrueInstruction: Instruction,
            onFalseInstruction: Instruction,
            edgeData: ValuesData.Defined,
            applyOnlyForThenBlock: Boolean = false
    ): ValuesData =
        when (conditionBoolValue) {
            is BooleanVariableValue.True -> {
                if (onFalseInstruction == currentInstruction && !applyOnlyForThenBlock)
                    // We are in "else" block and condition evaluated to "true" so this block will not
                    // be processed (dead code block). To indicate this we return values data dead
                    ValuesData.Dead
                else edgeData
            }
            is BooleanVariableValue.False -> {
                if (onTrueInstruction == currentInstruction)
                    // We are in "then" block and condition evaluated to "false" so this block will not
                    // be processed (dead code block). To indicate this we return values data dead
                    ValuesData.Dead
                else edgeData
            }
            is BooleanVariableValue.Undefined -> {
                fun processUndefinedCase(edgeData: ValuesData.Defined, restrictions: Map<VariableDescriptor, Set<Int>>) {
                    for ((variable, unrestrictedValues) in restrictions) {
                        val (values, sourceCollection) =
                                if (edgeData.intVarsToValues.contains(variable))
                                    edgeData.intVarsToValues[variable] to edgeData.intVarsToValues
                                else edgeData.collectionsToSizes[variable] to edgeData.collectionsToSizes
                        if (values is IntegerVariableValues.Defined) {
                            sourceCollection[variable] = values.leaveOnlyValuesInSet(unrestrictedValues)
                        }
                    }
                }
                if (onTrueInstruction == currentInstruction) {
                    // We are in "then" block and need to apply onTrue restrictions
                    processUndefinedCase(edgeData, conditionBoolValue.onTrueRestrictions)
                }
                else if (!applyOnlyForThenBlock) {
                    assert(onFalseInstruction == currentInstruction)
                    // We are in "else" block (and it's processing is required) and need to apply onFalse restrictions
                    processUndefinedCase(edgeData, conditionBoolValue.onFalseRestrictions)
                }
                edgeData
            }
        }

    private fun mergeVariablesValues(instruction: Instruction, incomingEdgesData: Collection<ValuesData>): Edges<ValuesData> {
        if (instruction is SubroutineSinkInstruction) {
            // this instruction is assumed to be the last one in function so it is not processed
            return Edges(ValuesData.Defined(), ValuesData.Defined())
        }
        val enterInstructionData = unionIncomingVariablesValues(incomingEdgesData)
        val exitInstructionData = updateValues(instruction, enterInstructionData)
        return Edges(enterInstructionData, exitInstructionData)
    }

    private fun unionIncomingVariablesValues(incomingEdgesData: Collection<ValuesData>): ValuesData =
        if (incomingEdgesData.isEmpty()) ValuesData.Defined()
        else incomingEdgesData.reduce { vd1, vd2 ->
                if (vd1 is ValuesData.Defined) {
                    if (vd2 is ValuesData.Defined) {
                        MapUtils.mergeMapsIntoFirst(vd1.intVarsToValues, vd2.intVarsToValues) { value1, value2 -> value1 merge value2 }
                        assert(vd1.intFakeVarsToValues.all { vd2.intFakeVarsToValues[it.key]?.equals(it.value) ?: true },
                               "intFake variables assumption failed")
                        vd1.intFakeVarsToValues.putAll(vd2.intFakeVarsToValues)
                        MapUtils.mergeMapsIntoFirst(vd1.boolVarsToValues, vd2.boolVarsToValues) { value1, value2 -> value1.or(value2) }
                        assert(vd1.boolFakeVarsToValues.all { vd2.boolFakeVarsToValues[it.key]?.equals(it.value) ?: true },
                               "boolFake variables assumption failed")
                        vd1.boolFakeVarsToValues.putAll(vd2.boolFakeVarsToValues)
                        MapUtils.mergeMapsIntoFirst(vd1.collectionsToSizes, vd2.collectionsToSizes) { value1, value2 -> value1 merge value2 }
                        vd1
                    }
                    else vd1
                }
                else vd2
            }

    private fun updateValues(instruction: Instruction, mergedEdgesData: ValuesData): ValuesData =
        if (mergedEdgesData is ValuesData.Defined) {
            val updatedData = mergedEdgesData.copy()
            val variablesUpdatedInLoop = loopsInfo.updatedVariables[instruction]
            if (variablesUpdatedInLoop != null) {
                // we meet the loop enter instruction
                processVariablesUpdatedInLoop(instruction, variablesUpdatedInLoop, updatedData)
            }
            when (instruction) {
                is VariableDeclarationInstruction -> processVariableDeclaration(instruction, updatedData)
                is ReadValueInstruction -> {
                    val element = instruction.outputValue.element
                    when (element) {
                        is JetConstantExpression -> processLiteral(element, instruction, updatedData)
                        is JetNameReferenceExpression -> processVariableReference(instruction, updatedData)
                    }
                }
                is WriteValueInstruction -> processAssignmentToVariable(instruction, updatedData)
                is CallInstruction -> {
                    when {
                        instruction.element is JetBinaryExpression -> // todo: make this check stronger, check args types
                            processBinaryOperation(instruction.element.operationToken, instruction, updatedData)
                        instruction.element is JetPrefixExpression -> // todo: make this check stronger, check args types
                            processUnaryOperation(instruction.element.operationToken, instruction, updatedData)
                        instruction.element is JetPostfixExpression -> // todo: make this check stronger, check args types
                            processUnaryOperation(instruction.element.operationToken, instruction, updatedData)
                        else -> {
                            val pseudoAnnotation = CallInstructionUtils.tryExtractPseudoAnnotationForCollector(instruction)
                            when (pseudoAnnotation) {
                                null -> Unit
                                is PseudoAnnotation.ConstructorWithSizeAsArg,
                                is PseudoAnnotation.ConstructorWithElementsAsArgs ->
                                    processCollectionCreation(pseudoAnnotation, instruction, updatedData)
                                is PseudoAnnotation.SizeMethod ->
                                    processSizeMethodCallOnCollection(instruction, updatedData)
                                is PseudoAnnotation.IncreaseSizeByConstantMethod -> {
                                    val numberOfElementsToAdd = IntegerVariableValues.Defined(pseudoAnnotation.increaseBy)
                                    processIncreaseSizeMethodCallOnCollection(numberOfElementsToAdd, instruction, updatedData)
                                }
                                is PseudoAnnotation.IncreaseSizeByPassedCollectionMethod -> {
                                    val numberOfElementsToAdd = tryExtractPassedCollectionSizes(pseudoAnnotation, instruction, updatedData)
                                    processIncreaseSizeMethodCallOnCollection(numberOfElementsToAdd, instruction, updatedData)
                                }
                                is PseudoAnnotation.DecreaseSizeToZeroMethod ->
                                    processDecreaseSizeMethodCallOnCollection(pseudoAnnotation, instruction, updatedData)
                            }
                        }
                    }
                }
                is MagicInstruction -> {
                    when (instruction.kind) {
                        MagicKind.LOOP_RANGE_ITERATION -> processLoopRangeInstruction(instruction, updatedData)
                        MagicKind.AND, MagicKind.OR -> {
                            // && and || operations are represented as MagicInstruction for some reason (not as CallInstruction)
                            if (instruction.element is JetBinaryExpression) {
                                processBinaryOperation(instruction.element.operationToken, instruction, updatedData)
                            }
                        }
                        MagicKind.EQUALS_IN_WHEN_CONDITION -> {
                            processBinaryOperation(JetTokens.EQEQ, instruction, updatedData)
                        }
                        else -> Unit
                    }
                }
            }
            removeFakeVariablesIfNeeded(instruction, updatedData)
            updatedData
        }
        else mergedEdgesData.copy()

    private fun processVariablesUpdatedInLoop(instruction: Instruction, variablesUpdatedInLoop: Set<VariableDescriptor>, edgeData: ValuesData.Defined) {
        fun processIntValues(descriptor: VariableDescriptor, varsMap: MutableMap<VariableDescriptor, IntegerVariableValues>): Boolean =
            if (varsMap.contains(descriptor)) {
                varsMap[descriptor] = IntegerVariableValues.Undefined
                true
            }
            else false
        fun processBoolValues(descriptor: VariableDescriptor, varsMap: MutableMap<VariableDescriptor, BooleanVariableValue>): Boolean {
            val value = varsMap[descriptor] ?: return false
            if (value is BooleanVariableValue.True || value is BooleanVariableValue.False)
                edgeData.boolVarsToValues[descriptor] = BooleanVariableValue.Undefined.WITH_NO_RESTRICTIONS
            return true
        }
        if (instruction is ConditionalJumpInstruction &&
            (instruction.element is JetWhileExpression || instruction.element is JetDoWhileExpression) &&
            instruction.conditionValue != null) {
            val conditionBooleanValue = edgeData.boolFakeVarsToValues[instruction.conditionValue]
            if (conditionBooleanValue is BooleanVariableValue.False) {
                // In case of while, it's body will be executed 0 times.
                // In case of do-while, it's body will be executed ones.
                // In both cases we no need to do anything with variables updated in this loop (see below).
                return
            }
        }
        variablesUpdatedInLoop.forEach {
            if (processIntValues(it, edgeData.intVarsToValues))
            else if (processBoolValues(it, edgeData.boolVarsToValues))
            else processIntValues(it, edgeData.collectionsToSizes)
        }
    }

    private fun processVariableDeclaration(instruction: Instruction, updatedData: ValuesData.Defined) {
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        val variableType = variableDescriptor.type
        when {
            KotlinBuiltIns.isInt(variableType) ->
                updatedData.intVarsToValues[variableDescriptor] = IntegerVariableValues.Uninitialized
            KotlinBuiltIns.isBoolean(variableType) ->
                updatedData.boolVarsToValues[variableDescriptor] = BooleanVariableValue.Undefined.WITH_NO_RESTRICTIONS
            KotlinArrayUtils.isGenericOrPrimitiveArray(variableType),
            KotlinListUtils.isKotlinList(variableType) ->
                updatedData.collectionsToSizes[variableDescriptor] = IntegerVariableValues.Uninitialized
        }
    }

    private fun processLiteral(element: JetConstantExpression, instruction: ReadValueInstruction, updatedData: ValuesData.Defined) {
        // process literal occurrence (all literals are stored to fake variables by read instruction)
        val node = element.node
        val nodeType = node.elementType as? JetNodeType
                       ?: return
        val fakeVariable = instruction.outputValue
        val valueAsText = node.text
        when (nodeType) {
            JetNodeTypes.INTEGER_CONSTANT ->
                try {
                    val literalValue = valueAsText.toInt()
                    updatedData.intFakeVarsToValues[fakeVariable] = IntegerVariableValues.Defined(literalValue)
                } catch (e: NumberFormatException) { /* not an int literal, so we don't need to do anything with it */ }
            JetNodeTypes.BOOLEAN_CONSTANT -> {
                val booleanValue = valueAsText.toBoolean()
                updatedData.boolFakeVarsToValues.put(fakeVariable, BooleanVariableValue.create(booleanValue))
            }
        }
    }

    private fun processVariableReference(instruction: ReadValueInstruction, updatedData: ValuesData.Defined) {
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        fun copyToFakes<V>(from: Map<VariableDescriptor, V>, to: MutableMap<PseudoValue, V>, copy: (V) -> V): Boolean {
            val valueToCopy = from[variableDescriptor] ?: return false
            val newFakeVariable = instruction.outputValue
            to[newFakeVariable] = copy(valueToCopy)
            return true
        }
        if (copyToFakes(updatedData.intVarsToValues, updatedData.intFakeVarsToValues) { it.copy() }) return
        else if (copyToFakes(updatedData.boolVarsToValues, updatedData.boolFakeVarsToValues) { it.copy() }) return
        else (copyToFakes(updatedData.collectionsToSizes, updatedData.intFakeVarsToValues) { it.copy() })
    }

    private fun processAssignmentToVariable(instruction: WriteValueInstruction, updatedData: ValuesData.Defined) {
        // process assignment to variable
        val variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext)
                                 ?: return
        val fakeVariable = instruction.rValue
        val targetType = tryGetTargetDescriptor(instruction.target)?.returnType
                         ?: return
        when {
            KotlinBuiltIns.isInt(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues[fakeVariable]?.let { it.copy() }
                                     ?: IntegerVariableValues.Undefined
                updatedData.intVarsToValues[variableDescriptor] = valuesToAssign
            }
            KotlinBuiltIns.isBoolean(targetType) -> {
                val valueToAssign = updatedData.boolFakeVarsToValues[fakeVariable]?.let { it.copy() }
                                    ?: BooleanVariableValue.Undefined.WITH_NO_RESTRICTIONS
                updatedData.boolVarsToValues[variableDescriptor] = valueToAssign
            }
            KotlinArrayUtils.isGenericOrPrimitiveArray(targetType),
            KotlinListUtils.isKotlinList(targetType) -> {
                val valuesToAssign = updatedData.intFakeVarsToValues[fakeVariable]?.let { it.copy() }
                                     ?: IntegerVariableValues.Undefined
                updatedData.collectionsToSizes[variableDescriptor] = valuesToAssign
            }
        }
    }

    private fun tryGetTargetDescriptor(target: AccessTarget): CallableDescriptor? {
        return when(target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.resultingDescriptor
            else -> null
        }
    }

    private fun processLoopRangeInstruction(instruction: MagicInstruction, updatedData: ValuesData.Defined) {
        // process range operator result storing in fake variable
        val rangeValues =
                if (!instruction.inputValues.isEmpty()) {
                    val rightOperandOfInExpr = instruction.inputValues[0]
                    val sourceInstruction = rightOperandOfInExpr.createdAt ?: null
                    if (sourceInstruction is CallInstruction &&
                        sourceInstruction.element is JetBinaryExpression &&
                        sourceInstruction.element.operationToken == JetTokens.RANGE)
                        // in `i in something` expressions `something` can only be `x .. y`
                        updatedData.intFakeVarsToValues[rightOperandOfInExpr]
                    else null
                }
                else null
        rangeValues?.let {
            val target = instruction.outputValue
            updatedData.intFakeVarsToValues.put(target, it)
        }
    }

    private fun processBinaryOperation(token: IElementType, instruction: OperationInstruction, updatedData: ValuesData.Defined) {
        if(instruction.inputValues.size() < 2) {
            // If the code under processing contains error (for example val a = x + 1, where variable x is undefined)
            // the binary operation may have less than 2 arguments
            return;
        }
        val leftOperandVariable = instruction.inputValues[0]
        val rightOperandVariable = instruction.inputValues[1]
        val resultVariable = instruction.outputValue
                             ?: return
        fun performOperation<Op, R>(
                operandsMap: MutableMap<PseudoValue, Op>,
                resultMap: MutableMap<PseudoValue, R>,
                valueToUseIfNoOperands: R,
                operation: (Op, Op) -> R
        ) {
            val leftOperandValues = operandsMap[leftOperandVariable]
            val rightOperandValues = operandsMap[rightOperandVariable]
            if (leftOperandValues != null && rightOperandValues != null) {
                resultMap[resultVariable] = operation(leftOperandValues, rightOperandValues)
            }
            else {
                resultMap[resultVariable] = valueToUseIfNoOperands
            }
        }
        fun intIntOperation(operation: (IntegerVariableValues, IntegerVariableValues) -> IntegerVariableValues) =
            performOperation(updatedData.intFakeVarsToValues, updatedData.intFakeVarsToValues,
                             IntegerVariableValues.Undefined, operation)
        fun intBoolOperation(operation: (IntegerVariableValues, IntegerVariableValues) -> BooleanVariableValue) =
                performOperation(updatedData.intFakeVarsToValues, updatedData.boolFakeVarsToValues,
                                 BooleanVariableValue.Undefined.WITH_NO_RESTRICTIONS, operation)
        fun boolBoolOperation(operation: (BooleanVariableValue, BooleanVariableValue) -> BooleanVariableValue) =
                performOperation(updatedData.boolFakeVarsToValues, updatedData.boolFakeVarsToValues,
                                 BooleanVariableValue.Undefined.WITH_NO_RESTRICTIONS, operation)
        val leftOperandDescriptor = leftOperandVariable.createdAt?.let {
            val instructionWithDescriptor = getCollectionReadInstructionIfIsSizeMethodCall(it) ?: it
            PseudocodeUtil.extractVariableDescriptorIfAny(instructionWithDescriptor, false, bindingContext)
        }
        when (token) {
            JetTokens.PLUS -> intIntOperation { x, y -> x + y }
            JetTokens.MINUS -> intIntOperation { x, y -> x - y }
            JetTokens.MUL -> intIntOperation { x, y -> x * y }
            JetTokens.DIV -> intIntOperation { x, y -> x / y }
            JetTokens.RANGE -> intIntOperation { x, y -> x .. y }
            JetTokens.EQEQ -> intBoolOperation { x, y -> x.eq(y, leftOperandDescriptor, updatedData) }
            JetTokens.EXCLEQ -> intBoolOperation { x, y -> x.notEq(y, leftOperandDescriptor, updatedData) }
            JetTokens.LT -> intBoolOperation { x, y ->  x.lessThan(y, leftOperandDescriptor, updatedData) }
            JetTokens.GT -> intBoolOperation { x, y -> x.greaterThan(y, leftOperandDescriptor, updatedData) }
            JetTokens.LTEQ -> intBoolOperation { x, y -> x.lessOrEq(y, leftOperandDescriptor, updatedData) }
            JetTokens.GTEQ -> intBoolOperation { x, y -> x.greaterOrEq(y, leftOperandDescriptor, updatedData) }
            JetTokens.OROR -> boolBoolOperation { x, y -> x.or(y) }
            JetTokens.ANDAND -> boolBoolOperation { x, y -> x.and(y) }
        }
    }

    private fun getCollectionReadInstructionIfIsSizeMethodCall(instruction: Instruction): Instruction? {
        if (instruction is CallInstruction && !instruction.inputValues.isEmpty()) {
            val pseudoAnnotation = CallInstructionUtils.tryExtractPseudoAnnotationForCollector(instruction) ?: return null
            if (pseudoAnnotation is PseudoAnnotation.SizeMethod) {
                return instruction.inputValues.first().createdAt
            }
        }
        return null
    }

    private fun processUnaryOperation(operationToken: IElementType, instruction: CallInstruction, updatedData: ValuesData.Defined) {
        if(instruction.inputValues.size() < 1) {
            // If the code under processing contains error (for example val a = ++)
            // the unary operation may have less than 1 argument
            return;
        }
        val operandVariable = instruction.inputValues[0]
        val resultVariable = instruction.outputValue
                             ?: return
        fun performOperation<V>(fakeVariablesMap: MutableMap<PseudoValue, V>, valueToUseIfNoOperands: V, operation: (V) -> V) {
            val operandValues = fakeVariablesMap[operandVariable]
            fakeVariablesMap[resultVariable] = operandValues?.let { operation(it) } ?: valueToUseIfNoOperands
        }
        when (operationToken) {
            JetTokens.MINUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) { -it }
            JetTokens.PLUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) { it.copy() }
            JetTokens.EXCL -> performOperation(updatedData.boolFakeVarsToValues, BooleanVariableValue.Undefined.WITH_NO_RESTRICTIONS) { !it }
            JetTokens.PLUSPLUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) {
                it + IntegerVariableValues.Defined(1)
            }
            JetTokens.MINUSMINUS -> performOperation(updatedData.intFakeVarsToValues, IntegerVariableValues.Undefined) {
                it - IntegerVariableValues.Defined(1)
            }
        }
    }

    private fun processCollectionCreation(pseudoAnnotation: PseudoAnnotation, instruction: CallInstruction, updatedData: ValuesData.Defined) {
        val collectionSize = tryExtractCollectionSize(pseudoAnnotation, instruction, updatedData)
        val collectionSizeVariable = instruction.outputValue
        if (collectionSizeVariable != null && collectionSize != null) {
            updatedData.intFakeVarsToValues[collectionSizeVariable] = collectionSize
        }
    }

    private fun tryExtractCollectionSize(
            pseudoAnnotation: PseudoAnnotation,
            instruction: CallInstruction,
            valuesData: ValuesData.Defined
    ): IntegerVariableValues? =
            when (pseudoAnnotation) {
                is PseudoAnnotation.ConstructorWithElementsAsArgs ->
                    IntegerVariableValues.Defined(instruction.arguments.size())
                is PseudoAnnotation.ConstructorWithSizeAsArg -> {
                    if (instruction.inputValues.size() > pseudoAnnotation.sizeArgPosition && pseudoAnnotation.sizeArgPosition >= 0) {
                        valuesData.intFakeVarsToValues[instruction.inputValues[pseudoAnnotation.sizeArgPosition]]
                    }
                    else {
                        // Code possibly contains error (like Array<Int>())
                        // so we can't define size
                        null
                    }
                }
                else -> null
            }

    private fun processSizeMethodCallOnCollection(instruction: CallInstruction, updatedData: ValuesData.Defined) {
        if (!instruction.inputValues.isEmpty()) {
            val collectionSize = updatedData.intFakeVarsToValues[instruction.inputValues[0]]
            val resultVariable = instruction.outputValue
            if (collectionSize != null && resultVariable != null) {
                updatedData.intFakeVarsToValues[resultVariable] = collectionSize
            }
        }
    }

    private fun processIncreaseSizeMethodCallOnCollection(
            numberOfElementsToAdd: IntegerVariableValues?,
            instruction: CallInstruction,
            updatedData: ValuesData.Defined
    ) {
        val collectionVariableDescriptor = tryExtractMethodCallReceiverDescriptor(instruction)
                                           ?: return
        val collectionSizes = updatedData.collectionsToSizes[collectionVariableDescriptor]
                              ?: return
        val updateValue = numberOfElementsToAdd?.let { collectionSizes + it } ?: IntegerVariableValues.Undefined
        updatedData.collectionsToSizes[collectionVariableDescriptor] = updateValue
    }

    private fun tryExtractPassedCollectionSizes(
            pseudoAnnotation: PseudoAnnotation.IncreaseSizeByPassedCollectionMethod,
            instruction: CallInstruction,
            updatedData: ValuesData.Defined
    ): IntegerVariableValues? {
        // This function is used to handle the code like `lst.addAll(lst2)`, when we know both `lst` and `lst2` sizes
        fun tryExtractFromKnownCollections(passedCollectionValueSourceInstruction: InstructionWithValue): IntegerVariableValues? {
            val passedCollectionVariableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                    passedCollectionValueSourceInstruction, false, bindingContext
            ) ?: return null
            return updatedData.collectionsToSizes[passedCollectionVariableDescriptor]
        }
        // This function is used to handle the code like `lst.addAll(arrayOf(...))`
        // so we check if the passed value is collection creation
        fun tryExtractFromInstruction(passedCollectionValueSourceInstruction: InstructionWithValue): IntegerVariableValues? {
            if (passedCollectionValueSourceInstruction is CallInstruction) {
                val pseudoAnnotationForCall = CallInstructionUtils.tryExtractPseudoAnnotationForCollector(passedCollectionValueSourceInstruction)
                                              ?: return null
                return tryExtractCollectionSize(pseudoAnnotationForCall, passedCollectionValueSourceInstruction, updatedData)
            }
            return null
        }
        // "+ 1" below is used because the first element of inputValue list is dispatch receiver
        val passedCollectionPosition = pseudoAnnotation.collectionArgPosition + 1
        if (instruction.inputValues.size() > passedCollectionPosition && passedCollectionPosition >= 0) {
            val passedCollectionValueSourceInstruction = instruction.inputValues[passedCollectionPosition].createdAt
                                                         ?: return null
            val sizeFromKnownCollections = tryExtractFromKnownCollections(passedCollectionValueSourceInstruction)
            if (sizeFromKnownCollections != null) {
                return sizeFromKnownCollections
            }
            return tryExtractFromInstruction(passedCollectionValueSourceInstruction)
        }
        return null
    }

    private fun processDecreaseSizeMethodCallOnCollection(
            pseudoAnnotation: PseudoAnnotation,
            instruction: CallInstruction,
            updatedData: ValuesData.Defined
    ) {
        val collectionVariableDescriptor = tryExtractMethodCallReceiverDescriptor(instruction)
                                           ?: return
        when(pseudoAnnotation) {
            is PseudoAnnotation.DecreaseSizeToZeroMethod ->
                updatedData.collectionsToSizes[collectionVariableDescriptor] = IntegerVariableValues.Defined(0)
        }
    }

    private fun tryExtractMethodCallReceiverDescriptor(instruction: CallInstruction): VariableDescriptor? {
        if (instruction.inputValues.size() > 0) {
            val collectionVariableValueSourceInstruction = instruction.inputValues[0].createdAt
                                                           ?: return null
            return PseudocodeUtil.extractVariableDescriptorIfAny(
                    collectionVariableValueSourceInstruction, false, bindingContext)
        }
        return null
    }

    // For each input fake variable of current instruction checks if the instruction is the last one to
    // use the variable as input. If this is true, the variable is removed (it will not be used anywhere anymore,
    // so it is useless).
    // Note that ConditionalJumpInstruction and NondeterministicJumpInstruction are not checked,
    // because it's input value is used in `applyRestrictionsOnValuesIfNeeded` function above after the instruction
    // of these types is processed. So in general it is impossible to determine when to remove the input value of these
    // instructions using `pseudocode.getUsages` info.
    private fun removeFakeVariablesIfNeeded(currentInstruction: Instruction, edgeData: ValuesData.Defined) {
        if (currentInstruction !is ConditionalJumpInstruction && currentInstruction !is NondeterministicJumpInstruction) {
            currentInstruction.inputValues.forEach {
                val valueUsages = pseudocode.getUsages(it)
                if (valueUsages.size() > 0 && valueUsages.last() == currentInstruction) {
                    edgeData.intFakeVarsToValues.remove(it) ?:
                    edgeData.boolFakeVarsToValues.remove(it)
                }
            }
        }
    }
}