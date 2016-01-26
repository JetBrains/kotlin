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

package org.jetbrains.kotlin.cfg.pseudocode

import com.google.common.collect.*
import com.intellij.util.containers.BidirectionalMap
import org.jetbrains.kotlin.cfg.Label
import org.jetbrains.kotlin.cfg.pseudocode.instructions.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MergeInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.AbstractJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.NondeterministicJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineEnterInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.*
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraverseInstructionResult
import org.jetbrains.kotlin.psi.KtElement

import java.util.*

import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.BACKWARD
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD

class PseudocodeImpl(override val correspondingElement: KtElement) : Pseudocode {

    inner class PseudocodeLabel internal constructor(private val name: String, private val comment: String?) : Label {
        var targetInstructionIndex: Int? = null
            private set

        override fun getName(): String {
            return name
        }

        override fun toString(): String {
            return if (comment == null) name else "$name [$comment]"
        }

        fun setTargetInstructionIndex(targetInstructionIndex: Int) {
            this.targetInstructionIndex = targetInstructionIndex
        }

        fun resolveToInstruction(): Instruction {
            assert(targetInstructionIndex != null)
            return mutableInstructionList[targetInstructionIndex!!]
        }

        fun copy(newLabelIndex: Int): PseudocodeLabel {
            return PseudocodeLabel("L" + newLabelIndex, "copy of $name, $comment")
        }

        val pseudocode: PseudocodeImpl
            get() = this@PseudocodeImpl
    }

    private val mutableInstructionList = ArrayList<Instruction>()
    override val instructions = ArrayList<Instruction>()

    private val elementsToValues = BidirectionalMap<KtElement, PseudoValue>()

    private val valueUsages = Maps.newHashMap<PseudoValue, MutableList<Instruction>>()
    private val mergedValues = Maps.newHashMap<PseudoValue, Set<PseudoValue>>()
    private val sideEffectFree = Sets.newHashSet<Instruction>()

    override var parent: Pseudocode? = null
        private set

    override val localDeclarations: Set<LocalFunctionDeclarationInstruction> by lazy {
        getLocalDeclarations(this)
    }
    //todo getters
    private val representativeInstructions = HashMap<KtElement, Instruction>()

    private val labels = ArrayList<PseudocodeLabel>()

    private var internalExitInstruction: SubroutineExitInstruction? = null

    override val exitInstruction: SubroutineExitInstruction
        get() = internalExitInstruction ?: throw AssertionError("Exit instruction is read before initialization")

    private var internalSinkInstruction: SubroutineSinkInstruction? = null

    override val sinkInstruction: SubroutineSinkInstruction
        get() = internalSinkInstruction ?: throw AssertionError("Sink instruction is read before initialization")

    private var internalErrorInstruction: SubroutineExitInstruction? = null

    private val errorInstruction: SubroutineExitInstruction
        get() = internalErrorInstruction ?: throw AssertionError("Error instruction is read before initialization")

    private var postPrecessed = false

    private fun getLocalDeclarations(pseudocode: Pseudocode): Set<LocalFunctionDeclarationInstruction> {
        val localDeclarations = Sets.newLinkedHashSet<LocalFunctionDeclarationInstruction>()
        for (instruction in (pseudocode as PseudocodeImpl).mutableInstructionList) {
            if (instruction is LocalFunctionDeclarationInstruction) {
                localDeclarations.add(instruction)
                localDeclarations.addAll(getLocalDeclarations(instruction.body))
            }
        }
        return localDeclarations
    }

    val rootPseudocode: Pseudocode
        get() {
            var parent = parent
            while (parent != null) {
                if (parent.parent == null) return parent
                parent = parent.parent
            }
            return this
        }

    fun createLabel(name: String, comment: String?): PseudocodeLabel {
        val label = PseudocodeLabel(name, comment)
        labels.add(label)
        return label
    }

    override val reversedInstructions: List<Instruction>
        get() {
            val traversedInstructions = Sets.newLinkedHashSet<Instruction>()
            traverseFollowingInstructions(sinkInstruction, traversedInstructions, BACKWARD, null)
            if (traversedInstructions.size < instructions.size) {
                val simplyReversedInstructions = Lists.newArrayList(instructions)
                Collections.reverse(simplyReversedInstructions)
                for (instruction in simplyReversedInstructions) {
                    if (!traversedInstructions.contains(instruction)) {
                        traverseFollowingInstructions(instruction, traversedInstructions, BACKWARD, null)
                    }
                }
            }
            return Lists.newArrayList(traversedInstructions)
        }

    override val instructionsIncludingDeadCode: List<Instruction>
        get() = mutableInstructionList

    //for tests only
    fun getLabels(): List<PseudocodeLabel> {
        return labels
    }

    fun addExitInstruction(exitInstruction: SubroutineExitInstruction) {
        addInstruction(exitInstruction)
        assert(internalExitInstruction == null) {
            "Repeated initialization of exit instruction: $internalExitInstruction --> $exitInstruction"
        }
        internalExitInstruction = exitInstruction
    }

    fun addSinkInstruction(sinkInstruction: SubroutineSinkInstruction) {
        addInstruction(sinkInstruction)
        assert(internalSinkInstruction == null) {
            "Repeated initialization of sink instruction: $internalSinkInstruction --> $sinkInstruction"
        }
        internalSinkInstruction = sinkInstruction
    }

    fun addErrorInstruction(errorInstruction: SubroutineExitInstruction) {
        addInstruction(errorInstruction)
        assert(internalErrorInstruction == null) {
            "Repeated initialization of error instruction: $internalErrorInstruction --> $errorInstruction"
        }
        internalErrorInstruction = errorInstruction
    }

    fun addInstruction(instruction: Instruction) {
        mutableInstructionList.add(instruction)
        instruction.owner = this

        if (instruction is KtElementInstruction) {
            representativeInstructions.put(instruction.element, instruction)
        }

        if (instruction is MergeInstruction) {
            addMergedValues(instruction)
        }

        for (inputValue in instruction.inputValues) {
            addValueUsage(inputValue, instruction)
            for (mergedValue in getMergedValues(inputValue)) {
                addValueUsage(mergedValue, instruction)
            }
        }
        if (instruction.calcSideEffectFree()) {
            sideEffectFree.add(instruction)
        }
    }

    override val enterInstruction: SubroutineEnterInstruction
        get() = mutableInstructionList[0] as SubroutineEnterInstruction

    override fun getElementValue(element: KtElement?) = elementsToValues[element]

    override fun getValueElements(value: PseudoValue?) = elementsToValues.getKeysByValue(value) ?: emptyList()

    override fun getUsages(value: PseudoValue?) = valueUsages[value] ?: mutableListOf()

    override fun isSideEffectFree(instruction: Instruction) = sideEffectFree.contains(instruction)

    fun bindElementToValue(element: KtElement, value: PseudoValue) {
        elementsToValues.put(element, value)
    }

    fun bindLabel(label: Label) {
        (label as PseudocodeLabel).setTargetInstructionIndex(mutableInstructionList.size)
    }

    private fun getMergedValues(value: PseudoValue) = mergedValues[value] ?: emptySet()

    private fun addMergedValues(instruction: MergeInstruction) {
        val result = LinkedHashSet<PseudoValue>()
        for (value in instruction.inputValues) {
            result.addAll(getMergedValues(value))
            result.add(value)
        }
        mergedValues.put(instruction.outputValue, result)
    }

    private fun addValueUsage(value: PseudoValue, usage: Instruction) {
        if (usage is MergeInstruction) return
        valueUsages.getOrPut(
                value
        ) { Lists.newArrayList<Instruction>() }.add(usage)
    }

    fun postProcess() {
        if (postPrecessed) return
        postPrecessed = true
        errorInstruction.sink = sinkInstruction
        exitInstruction.sink = sinkInstruction
        var index = 0
        for (instruction in mutableInstructionList) {
            //recursively invokes 'postProcess' for local declarations
            processInstruction(instruction, index)
            index++
        }
        if (parent != null) return

        // Collecting reachable instructions should be done after processing all instructions
        // (including instructions in local declarations) to avoid being in incomplete state.
        collectAndCacheReachableInstructions()
        for (localFunctionDeclarationInstruction in localDeclarations) {
            (localFunctionDeclarationInstruction.body as PseudocodeImpl).collectAndCacheReachableInstructions()
        }
    }

    private fun collectAndCacheReachableInstructions() {
        val reachableInstructions = collectReachableInstructions()
        for (instruction in mutableInstructionList) {
            if (reachableInstructions.contains(instruction)) {
                instructions.add(instruction)
            }
        }
        markDeadInstructions()
    }

    private fun processInstruction(instruction: Instruction, currentPosition: Int) {
        instruction.accept(object : InstructionVisitor() {
            override fun visitInstructionWithNext(instruction: InstructionWithNext) {
                instruction.next = getNextPosition(currentPosition)
            }

            override fun visitJump(instruction: AbstractJumpInstruction) {
                instruction.resolvedTarget = getJumpTarget(instruction.targetLabel)
            }

            override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                instruction.next = getNextPosition(currentPosition)
                val targetLabels = instruction.targetLabels
                for (targetLabel in targetLabels) {
                    instruction.setResolvedTarget(targetLabel, getJumpTarget(targetLabel))
                }
            }

            override fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
                val nextInstruction = getNextPosition(currentPosition)
                val jumpTarget = getJumpTarget(instruction.targetLabel)
                if (instruction.onTrue) {
                    instruction.nextOnFalse = nextInstruction
                    instruction.nextOnTrue = jumpTarget
                }
                else {
                    instruction.nextOnFalse = jumpTarget
                    instruction.nextOnTrue = nextInstruction
                }
                visitJump(instruction)
            }

            override fun visitLocalFunctionDeclarationInstruction(instruction: LocalFunctionDeclarationInstruction) {
                val body = instruction.body as PseudocodeImpl
                body.parent = this@PseudocodeImpl
                body.postProcess()
                instruction.next = sinkInstruction
            }

            override fun visitSubroutineExit(instruction: SubroutineExitInstruction) {
                // Nothing
            }

            override fun visitSubroutineSink(instruction: SubroutineSinkInstruction) {
                // Nothing
            }

            override fun visitInstruction(instruction: Instruction) {
                throw UnsupportedOperationException(instruction.toString())
            }
        })
    }

    private fun collectReachableInstructions(): Set<Instruction> {
        val visited = Sets.newHashSet<Instruction>()
        traverseFollowingInstructions(enterInstruction, visited, FORWARD
        ) { instruction ->
            if (instruction is MagicInstruction && instruction.kind === MagicKind.EXHAUSTIVE_WHEN_ELSE) {
                return@traverseFollowingInstructions TraverseInstructionResult.SKIP
            }
            TraverseInstructionResult.CONTINUE
        }
        if (!visited.contains(exitInstruction)) {
            visited.add(exitInstruction)
        }
        if (!visited.contains(errorInstruction)) {
            visited.add(errorInstruction)
        }
        if (!visited.contains(sinkInstruction)) {
            visited.add(sinkInstruction)
        }
        return visited
    }

    private fun markDeadInstructions() {
        val instructionSet = Sets.newHashSet(instructions)
        for (instruction in mutableInstructionList) {
            if (!instructionSet.contains(instruction)) {
                (instruction as? InstructionImpl)?.markedAsDead = true
                for (nextInstruction in instruction.nextInstructions) {
                    (nextInstruction as? InstructionImpl)?.previousInstructions?.remove(instruction)
                }
            }
        }
    }

    private fun getJumpTarget(targetLabel: Label): Instruction {
        return (targetLabel as PseudocodeLabel).resolveToInstruction()
    }

    private fun getNextPosition(currentPosition: Int): Instruction {
        val targetPosition = currentPosition + 1
        assert(targetPosition < mutableInstructionList.size) { currentPosition }
        return mutableInstructionList[targetPosition]
    }

    override fun copy(): PseudocodeImpl {
        val result = PseudocodeImpl(correspondingElement)
        result.repeatWhole(this)
        return result
    }

    private fun repeatWhole(originalPseudocode: PseudocodeImpl) {
        repeatInternal(originalPseudocode, null, null, 0)
        parent = originalPseudocode.parent
    }

    fun repeatPart(startLabel: Label, finishLabel: Label, labelCount: Int): Int {
        return repeatInternal((startLabel as PseudocodeLabel).pseudocode, startLabel, finishLabel, labelCount)
    }

    private fun repeatInternal(
            originalPseudocode: PseudocodeImpl,
            startLabel: Label?, finishLabel: Label?,
            labelCountArg: Int): Int {
        var labelCount = labelCountArg
        val startIndex = (if (startLabel != null) (startLabel as PseudocodeLabel).targetInstructionIndex else Integer.valueOf(0))!!
        val finishIndex = (if (finishLabel != null)
            (finishLabel as PseudocodeLabel).targetInstructionIndex
        else
            Integer.valueOf(originalPseudocode.mutableInstructionList.size))!!

        val originalToCopy = Maps.newLinkedHashMap<Label, Label>()
        val originalLabelsForInstruction = HashMultimap.create<Instruction, Label>()
        for (label in originalPseudocode.labels) {
            val index = label.targetInstructionIndex ?: continue
            //label is not bounded yet
            if (label === startLabel || label === finishLabel) continue

            if (startIndex <= index && index <= finishIndex) {
                originalToCopy.put(label, label.copy(labelCount++))
                originalLabelsForInstruction.put(getJumpTarget(label), label)
            }
        }
        for (label in originalToCopy.values) {
            labels.add(label as PseudocodeLabel)
        }
        for (index in startIndex..finishIndex - 1) {
            val originalInstruction = originalPseudocode.mutableInstructionList[index]
            repeatLabelsBindingForInstruction(originalInstruction, originalToCopy, originalLabelsForInstruction)
            val copy = copyInstruction(originalInstruction, originalToCopy)
            addInstruction(copy)
            if (originalInstruction === originalPseudocode.internalErrorInstruction && copy is SubroutineExitInstruction) {
                internalErrorInstruction = copy
            }
            if (originalInstruction === originalPseudocode.internalExitInstruction && copy is SubroutineExitInstruction) {
                internalExitInstruction = copy
            }
            if (originalInstruction === originalPseudocode.internalSinkInstruction && copy is SubroutineSinkInstruction) {
                internalSinkInstruction = copy
            }
        }
        if (finishIndex < mutableInstructionList.size) {
            repeatLabelsBindingForInstruction(originalPseudocode.mutableInstructionList[finishIndex],
                                              originalToCopy,
                                              originalLabelsForInstruction)
        }
        return labelCount
    }

    private fun repeatLabelsBindingForInstruction(
            originalInstruction: Instruction,
            originalToCopy: Map<Label, Label>,
            originalLabelsForInstruction: Multimap<Instruction, Label>) {
        for (originalLabel in originalLabelsForInstruction.get(originalInstruction)) {
            bindLabel(originalToCopy[originalLabel]!!)
        }
    }

    private fun copyInstruction(instruction: Instruction, originalToCopy: Map<Label, Label>): Instruction {
        if (instruction is AbstractJumpInstruction) {
            val originalTarget = instruction.targetLabel
            if (originalToCopy.containsKey(originalTarget)) {
                return instruction.copy(originalToCopy[originalTarget]!!)
            }
        }
        if (instruction is NondeterministicJumpInstruction) {
            val originalTargets = instruction.targetLabels
            val copyTargets = copyLabels(originalTargets, originalToCopy)
            return instruction.copy(copyTargets)
        }
        return (instruction as InstructionImpl).copy()
    }

    private fun copyLabels(labels: Collection<Label>, originalToCopy: Map<Label, Label>): MutableList<Label> {
        val newLabels = Lists.newArrayList<Label>()
        for (label in labels) {
            val newLabel = originalToCopy[label]
            newLabels.add(newLabel ?: label)
        }
        return newLabels
    }
}
