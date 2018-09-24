/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cfg.variable

import org.jetbrains.kotlin.cfg.*
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
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils.variableDescriptorForDeclaration
import org.jetbrains.kotlin.util.javaslang.ImmutableHashMap
import org.jetbrains.kotlin.util.javaslang.ImmutableMap
import org.jetbrains.kotlin.util.javaslang.component1
import org.jetbrains.kotlin.util.javaslang.component2
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private typealias ImmutableSet<T> = javaslang.collection.Set<T>
private typealias ImmutableHashSet<T> = javaslang.collection.HashSet<T>

class PseudocodeVariablesData(val pseudocode: Pseudocode, private val bindingContext: BindingContext) {
    private val containsDoWhile = pseudocode.rootPseudocode.containsDoWhile
    private val pseudocodeVariableDataCollector =
        PseudocodeVariableDataCollector(bindingContext, pseudocode)

    private class VariablesForDeclaration(
        val valsWithTrivialInitializer: Set<VariableDescriptor>,
        val nonTrivialVariables: Set<VariableDescriptor>
    ) {
        val allVars =
            if (nonTrivialVariables.isEmpty())
                valsWithTrivialInitializer
            else
                LinkedHashSet(valsWithTrivialInitializer).also { it.addAll(nonTrivialVariables) }
    }

    private val declaredVariablesForDeclaration = hashMapOf<Pseudocode, VariablesForDeclaration>()
    private val rootVariables by lazy(LazyThreadSafetyMode.NONE) {
        getAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations = true)
    }

    val variableInitializers: Map<Instruction, Edges<ReadOnlyInitControlFlowInfo>> by lazy {
        computeVariableInitializers()
    }

    val blockScopeVariableInfo: BlockScopeVariableInfo
        get() = pseudocodeVariableDataCollector.blockScopeVariableInfo

    fun getDeclaredVariables(pseudocode: Pseudocode, includeInsideLocalDeclarations: Boolean): Set<VariableDescriptor> =
        getAllDeclaredVariables(pseudocode, includeInsideLocalDeclarations).allVars

    fun isVariableWithTrivialInitializer(variableDescriptor: VariableDescriptor) =
        variableDescriptor in rootVariables.valsWithTrivialInitializer

    private fun getAllDeclaredVariables(pseudocode: Pseudocode, includeInsideLocalDeclarations: Boolean): VariablesForDeclaration {
        if (!includeInsideLocalDeclarations) {
            return getUpperLevelDeclaredVariables(pseudocode)
        }
        val nonTrivialVariables = linkedSetOf<VariableDescriptor>()
        val valsWithTrivialInitializer = linkedSetOf<VariableDescriptor>()
        addVariablesFromPseudocode(pseudocode, nonTrivialVariables, valsWithTrivialInitializer)

        for (localFunctionDeclarationInstruction in pseudocode.localDeclarations) {
            val localPseudocode = localFunctionDeclarationInstruction.body
            addVariablesFromPseudocode(localPseudocode, nonTrivialVariables, valsWithTrivialInitializer)
        }
        return VariablesForDeclaration(
            valsWithTrivialInitializer,
            nonTrivialVariables
        )
    }

    private fun addVariablesFromPseudocode(
        pseudocode: Pseudocode,
        nonTrivialVariables: MutableSet<VariableDescriptor>,
        valsWithTrivialInitializer: MutableSet<VariableDescriptor>
    ) {
        getUpperLevelDeclaredVariables(pseudocode).let {
            nonTrivialVariables.addAll(it.nonTrivialVariables)
            valsWithTrivialInitializer.addAll(it.valsWithTrivialInitializer)
        }
    }

    private fun getUpperLevelDeclaredVariables(pseudocode: Pseudocode) = declaredVariablesForDeclaration.getOrPut(pseudocode) {
        computeDeclaredVariablesForPseudocode(pseudocode)
    }

    private fun computeDeclaredVariablesForPseudocode(pseudocode: Pseudocode): VariablesForDeclaration {
        val valsWithTrivialInitializer = linkedSetOf<VariableDescriptor>()
        val nonTrivialVariables = linkedSetOf<VariableDescriptor>()
        for (instruction in pseudocode.instructions) {
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor =
                    variableDescriptorForDeclaration(
                        bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                    ) ?: continue

                if (!containsDoWhile && isValWithTrivialInitializer(variableDeclarationElement, descriptor)) {
                    valsWithTrivialInitializer.add(descriptor)
                } else {
                    nonTrivialVariables.add(descriptor)
                }
            }
        }

        return VariablesForDeclaration(
            valsWithTrivialInitializer,
            nonTrivialVariables
        )
    }

    private fun isValWithTrivialInitializer(variableDeclarationElement: KtDeclaration, descriptor: VariableDescriptor) =
        variableDeclarationElement is KtParameter || variableDeclarationElement is KtObjectDeclaration ||
                variableDeclarationElement.safeAs<KtVariableDeclaration>()?.isVariableWithTrivialInitializer(descriptor) == true

    private fun KtVariableDeclaration.isVariableWithTrivialInitializer(descriptor: VariableDescriptor): Boolean {
        if (descriptor.isPropertyWithoutBackingField()) return true
        if (isVar) return false
        return initializer != null || safeAs<KtProperty>()?.delegate != null || this is KtDestructuringDeclarationEntry
    }

    private fun VariableDescriptor.isPropertyWithoutBackingField(): Boolean {
        if (this !is PropertyDescriptor) return false
        return bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) != true
    }

    // variable initializers

    private fun computeVariableInitializers(): Map<Instruction, Edges<ReadOnlyInitControlFlowInfo>> {

        val blockScopeVariableInfo = pseudocodeVariableDataCollector.blockScopeVariableInfo

        val resultForValsWithTrivialInitializer = computeInitInfoForTrivialVals()

        if (rootVariables.nonTrivialVariables.isEmpty()) return resultForValsWithTrivialInitializer

        return pseudocodeVariableDataCollector.collectData(
            TraversalOrder.FORWARD,
            InitControlFlowInfo()
        ) { instruction: Instruction, incomingEdgesData: Collection<InitControlFlowInfo> ->

            val enterInstructionData =
                mergeIncomingEdgesDataForInitializers(
                    instruction,
                    incomingEdgesData,
                    blockScopeVariableInfo
                )
            val exitInstructionData = addVariableInitStateFromCurrentInstructionIfAny(
                instruction, enterInstructionData, blockScopeVariableInfo
            )
            Edges(enterInstructionData, exitInstructionData)
        }.mapValues { (instruction, edges) ->
            val trivialEdges = resultForValsWithTrivialInitializer[instruction]!!
            Edges(trivialEdges.incoming.replaceDelegate(edges.incoming), trivialEdges.outgoing.replaceDelegate(edges.outgoing))
        }
    }

    private fun computeInitInfoForTrivialVals(): Map<Instruction, Edges<ReadOnlyInitControlFlowInfoImpl>> {
        val result = hashMapOf<Instruction, Edges<ReadOnlyInitControlFlowInfoImpl>>()
        var declaredSet = ImmutableHashSet.empty<VariableDescriptor>()
        var initSet = ImmutableHashSet.empty<VariableDescriptor>()
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            val enterState = ReadOnlyInitControlFlowInfoImpl(declaredSet, initSet, null)
            when (instruction) {
                is VariableDeclarationInstruction ->
                    extractValWithTrivialInitializer(instruction)?.let { variableDescriptor ->
                        declaredSet = declaredSet.add(variableDescriptor)
                    }
                is WriteValueInstruction -> {
                    val variableDescriptor = extractValWithTrivialInitializer(instruction)
                    if (variableDescriptor != null && instruction.isTrivialInitializer()) {
                        initSet = initSet.add(variableDescriptor)
                    }
                }
            }

            val afterState = ReadOnlyInitControlFlowInfoImpl(declaredSet, initSet, null)

            result[instruction] = Edges(enterState, afterState)
        }
        return result
    }

    private fun WriteValueInstruction.isTrivialInitializer() =
    // WriteValueInstruction having KtDeclaration as an element means
    // it must be a write happened at the same time when
    // the variable (common variable/parameter/object) has been declared
        element is KtDeclaration

    private inner class ReadOnlyInitControlFlowInfoImpl(
        val declaredSet: ImmutableSet<VariableDescriptor>,
        val initSet: ImmutableSet<VariableDescriptor>,
        private val delegate: ReadOnlyInitControlFlowInfo?
    ) : ReadOnlyInitControlFlowInfo {
        override fun getOrNull(variableDescriptor: VariableDescriptor): VariableControlFlowState? {
            if (variableDescriptor in declaredSet) {
                return VariableControlFlowState.create(
                    isInitialized = variableDescriptor in initSet,
                    isDeclared = true
                )
            }
            return delegate?.getOrNull(variableDescriptor)
        }

        override fun checkDefiniteInitializationInWhen(merge: ReadOnlyInitControlFlowInfo): Boolean =
            delegate?.checkDefiniteInitializationInWhen(merge) ?: false

        fun replaceDelegate(newDelegate: ReadOnlyInitControlFlowInfo): ReadOnlyInitControlFlowInfo =
            ReadOnlyInitControlFlowInfoImpl(declaredSet, initSet, newDelegate)

        override fun asMap(): ImmutableMap<VariableDescriptor, VariableControlFlowState> {
            val initial = delegate?.asMap() ?: ImmutableHashMap.empty()

            return declaredSet.fold(initial) { acc, variableDescriptor ->
                acc.put(variableDescriptor, getOrNull(variableDescriptor)!!)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ReadOnlyInitControlFlowInfoImpl

            if (declaredSet != other.declaredSet) return false
            if (initSet != other.initSet) return false
            if (delegate != other.delegate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = declaredSet.hashCode()
            result = 31 * result + initSet.hashCode()
            result = 31 * result + (delegate?.hashCode() ?: 0)
            return result
        }
    }

    private fun addVariableInitStateFromCurrentInstructionIfAny(
        instruction: Instruction,
        enterInstructionData: InitControlFlowInfo,
        blockScopeVariableInfo: BlockScopeVariableInfo
    ): InitControlFlowInfo {
        if (instruction is MagicInstruction) {
            if (instruction.kind === MagicKind.EXHAUSTIVE_WHEN_ELSE) {
                return enterInstructionData.iterator().fold(enterInstructionData) { result, (key, value) ->
                    if (!value.definitelyInitialized()) {
                        result.put(
                            key,
                            VariableControlFlowState.createInitializedExhaustively(value.isDeclared)
                        )
                    } else result
                }
            }
        }
        if (instruction !is WriteValueInstruction && instruction !is VariableDeclarationInstruction) {
            return enterInstructionData
        }
        val variable =
            PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)
                ?.takeIf { it in rootVariables.nonTrivialVariables }
                ?: return enterInstructionData
        var exitInstructionData = enterInstructionData
        if (instruction is WriteValueInstruction) {
            // if writing to already initialized object
            if (!PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, bindingContext)) {
                return enterInstructionData
            }

            val enterInitState = enterInstructionData.getOrNull(variable)
            val initializationAtThisElement =
                VariableControlFlowState.create(instruction.element is KtProperty, enterInitState)
            exitInstructionData = exitInstructionData.put(variable, initializationAtThisElement, enterInitState)
        } else {
            // instruction instanceof VariableDeclarationInstruction
            val enterInitState =
                enterInstructionData.getOrNull(variable)
                    ?: getDefaultValueForInitializers(
                        variable,
                        instruction,
                        blockScopeVariableInfo
                    )

            if (!enterInitState.mayBeInitialized() || !enterInitState.isDeclared) {
                val variableDeclarationInfo =
                    VariableControlFlowState.create(enterInitState.initState, isDeclared = true)
                exitInstructionData = exitInstructionData.put(variable, variableDeclarationInfo, enterInitState)
            }
        }
        return exitInstructionData
    }

    // variable use

    val variableUseStatusData: Map<Instruction, Edges<ReadOnlyUseControlFlowInfo>>
        get() {
            val edgesForTrivialVals = computeUseInfoForTrivialVals()
            if (rootVariables.nonTrivialVariables.isEmpty()) {
                return hashMapOf<Instruction, Edges<ReadOnlyUseControlFlowInfo>>().apply {
                    pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
                        put(instruction, edgesForTrivialVals)
                    }
                }
            }

            return pseudocodeVariableDataCollector.collectData(
                TraversalOrder.BACKWARD,
                UseControlFlowInfo()
            ) { instruction: Instruction, incomingEdgesData: Collection<UseControlFlowInfo> ->

                val enterResult: UseControlFlowInfo = if (incomingEdgesData.size == 1) {
                    incomingEdgesData.single()
                } else {
                    incomingEdgesData.fold(UseControlFlowInfo()) { result, edgeData ->
                        edgeData.iterator().fold(result) { subResult, (variableDescriptor, variableUseState) ->
                            subResult.put(variableDescriptor, variableUseState.merge(subResult.getOrNull(variableDescriptor)))
                        }
                    }
                }

                val variableDescriptor =
                    PseudocodeUtil.extractVariableDescriptorFromReference(instruction, bindingContext)
                        ?.takeIf { it in rootVariables.nonTrivialVariables }
                if (variableDescriptor == null || instruction !is ReadValueInstruction && instruction !is WriteValueInstruction) {
                    Edges(enterResult, enterResult)
                } else {
                    val exitResult =
                        if (instruction is ReadValueInstruction) {
                            enterResult.put(variableDescriptor, VariableUseState.READ)
                        } else {
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
            }.mapValues { (_, edges) ->
                Edges(
                    edgesForTrivialVals.incoming.replaceDelegate(edges.incoming),
                    edgesForTrivialVals.outgoing.replaceDelegate(edges.outgoing)
                )
            }
        }

    private fun computeUseInfoForTrivialVals(): Edges<ReadOnlyUseControlFlowInfoImpl> {
        val used = hashSetOf<VariableDescriptor>()

        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is ReadValueInstruction) {
                extractValWithTrivialInitializer(instruction)?.let {
                    used.add(it)
                }
            }
        }

        val constantUseInfo = ReadOnlyUseControlFlowInfoImpl(used, null)
        return Edges(constantUseInfo, constantUseInfo)
    }

    private fun extractValWithTrivialInitializer(instruction: Instruction): VariableDescriptor? {
        return PseudocodeUtil.extractVariableDescriptorIfAny(instruction, bindingContext)?.takeIf {
            it in rootVariables.valsWithTrivialInitializer
        }
    }

    private inner class ReadOnlyUseControlFlowInfoImpl(
        val used: Set<VariableDescriptor>,
        val delegate: ReadOnlyUseControlFlowInfo?
    ) : ReadOnlyUseControlFlowInfo {
        override fun getOrNull(variableDescriptor: VariableDescriptor): VariableUseState? {
            if (variableDescriptor in used) return VariableUseState.READ
            return delegate?.getOrNull(variableDescriptor)
        }

        fun replaceDelegate(newDelegate: ReadOnlyUseControlFlowInfo): ReadOnlyUseControlFlowInfo =
            ReadOnlyUseControlFlowInfoImpl(used, newDelegate)

        override fun asMap(): ImmutableMap<VariableDescriptor, VariableUseState> {
            val initial = delegate?.asMap() ?: ImmutableHashMap.empty()

            return used.fold(initial) { acc, variableDescriptor ->
                acc.put(variableDescriptor, getOrNull(variableDescriptor)!!)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ReadOnlyUseControlFlowInfoImpl

            if (used != other.used) return false
            if (delegate != other.delegate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = used.hashCode()
            result = 31 * result + (delegate?.hashCode() ?: 0)
            return result
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
                        ?: getDefaultValueForInitializers(
                            variable,
                            instruction,
                            blockScopeVariableInfo
                        )
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
