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

package org.jetbrains.kotlin.cfg;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.ReadValueInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.BACKWARD;
import static org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD;

public class PseudocodeVariablesData {
    private final Pseudocode pseudocode;
    private final BindingContext bindingContext;
    private final PseudocodeVariableDataCollector pseudocodeVariableDataCollector;

    private final Map<Pseudocode, Set<VariableDescriptor>> declaredVariablesForDeclaration = Maps.newHashMap();

    private Map<Instruction, Edges<Map<VariableDescriptor, VariableControlFlowState>>> variableInitializers;

    public PseudocodeVariablesData(@NotNull Pseudocode pseudocode, @NotNull BindingContext bindingContext) {
        this.pseudocode = pseudocode;
        this.bindingContext = bindingContext;
        this.pseudocodeVariableDataCollector = new PseudocodeVariableDataCollector(bindingContext, pseudocode);
    }

    @NotNull
    public Pseudocode getPseudocode() {
        return pseudocode;
    }

    @NotNull
    public LexicalScopeVariableInfo getLexicalScopeVariableInfo() {
        return pseudocodeVariableDataCollector.getLexicalScopeVariableInfo();
    }

    @NotNull
    public Set<VariableDescriptor> getDeclaredVariables(@NotNull Pseudocode pseudocode, boolean includeInsideLocalDeclarations) {
        if (!includeInsideLocalDeclarations) {
            return getUpperLevelDeclaredVariables(pseudocode);
        }
        Set<VariableDescriptor> declaredVariables = Sets.newHashSet();
        declaredVariables.addAll(getUpperLevelDeclaredVariables(pseudocode));

        for (LocalFunctionDeclarationInstruction localFunctionDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = localFunctionDeclarationInstruction.getBody();
            declaredVariables.addAll(getUpperLevelDeclaredVariables(localPseudocode));
        }
        return declaredVariables;
    }

    @NotNull
    private Set<VariableDescriptor> getUpperLevelDeclaredVariables(@NotNull Pseudocode pseudocode) {
        Set<VariableDescriptor> declaredVariables = declaredVariablesForDeclaration.get(pseudocode);
        if (declaredVariables == null) {
            declaredVariables = computeDeclaredVariablesForPseudocode(pseudocode);
            declaredVariablesForDeclaration.put(pseudocode, declaredVariables);
        }
        return declaredVariables;
    }

    @NotNull
    private Set<VariableDescriptor> computeDeclaredVariablesForPseudocode(Pseudocode pseudocode) {
        Set<VariableDescriptor> declaredVariables = Sets.newHashSet();
        for (Instruction instruction : pseudocode.getInstructions()) {
            if (instruction instanceof VariableDeclarationInstruction) {
                KtDeclaration variableDeclarationElement = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement);
                if (descriptor != null) {
                    assert descriptor instanceof VariableDescriptor;
                    declaredVariables.add((VariableDescriptor) descriptor);
                }
            }
        }
        return Collections.unmodifiableSet(declaredVariables);
    }

    // variable initializers

    @NotNull
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableControlFlowState>>> getVariableInitializers() {
        if (variableInitializers == null) {
            variableInitializers = computeVariableInitializers();
        }

        return variableInitializers;
    }

    @NotNull
    private Map<Instruction, Edges<Map<VariableDescriptor, VariableControlFlowState>>> computeVariableInitializers() {

        final LexicalScopeVariableInfo lexicalScopeVariableInfo = pseudocodeVariableDataCollector.getLexicalScopeVariableInfo();

        return pseudocodeVariableDataCollector.collectData(
                FORWARD, /*mergeDataWithLocalDeclarations=*/ false,
                new InstructionDataMergeStrategy<VariableControlFlowState>() {
                    @NotNull
                    @Override
                    public Edges<Map<VariableDescriptor, VariableControlFlowState>> invoke(
                            @NotNull Instruction instruction,
                            @NotNull Collection<? extends Map<VariableDescriptor, VariableControlFlowState>> incomingEdgesData
                    ) {

                        Map<VariableDescriptor, VariableControlFlowState> enterInstructionData =
                                mergeIncomingEdgesDataForInitializers(incomingEdgesData);
                        Map<VariableDescriptor, VariableControlFlowState> exitInstructionData = addVariableInitStateFromCurrentInstructionIfAny(
                                instruction, enterInstructionData, lexicalScopeVariableInfo);
                        return new Edges<Map<VariableDescriptor, VariableControlFlowState>>(enterInstructionData, exitInstructionData);
                    }
                }
        );
    }

    public static VariableControlFlowState getDefaultValueForInitializers(
            @NotNull VariableDescriptor variable,
            @NotNull Instruction instruction,
            @NotNull LexicalScopeVariableInfo lexicalScopeVariableInfo
    ) {
        //todo: think of replacing it with "MapWithDefaultValue"
        LexicalScope declaredIn = lexicalScopeVariableInfo.getDeclaredIn().get(variable);
        boolean declaredOutsideThisDeclaration =
                declaredIn == null //declared outside this pseudocode
                || declaredIn.getLexicalScopeForContainingDeclaration() != instruction.getLexicalScope().getLexicalScopeForContainingDeclaration();
        return VariableControlFlowState.create(/*initState=*/declaredOutsideThisDeclaration);
    }

    @NotNull
    private static Map<VariableDescriptor, VariableControlFlowState> mergeIncomingEdgesDataForInitializers(
            @NotNull Collection<? extends Map<VariableDescriptor, VariableControlFlowState>> incomingEdgesData
    ) {
        Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
        for (Map<VariableDescriptor, VariableControlFlowState> edgeData : incomingEdgesData) {
            variablesInScope.addAll(edgeData.keySet());
        }

        Map<VariableDescriptor, VariableControlFlowState> enterInstructionData = Maps.newHashMap();
        for (VariableDescriptor variable : variablesInScope) {
            TriInitState initState = null;
            boolean isDeclared = true;
            for (Map<VariableDescriptor, VariableControlFlowState> edgeData : incomingEdgesData) {
                VariableControlFlowState varControlFlowState = edgeData.get(variable);
                if (varControlFlowState != null) {
                    initState = initState != null ? initState.merge(varControlFlowState.initState) : varControlFlowState.initState;
                    if (!varControlFlowState.isDeclared) {
                        isDeclared = false;
                    }
                }
            }
            if (initState == null) {
                throw new AssertionError("An empty set of incoming edges data");
            }
            enterInstructionData.put(variable, VariableControlFlowState.create(initState, isDeclared));
        }
        return enterInstructionData;
    }

    @NotNull
    private Map<VariableDescriptor, VariableControlFlowState> addVariableInitStateFromCurrentInstructionIfAny(
            @NotNull Instruction instruction,
            @NotNull Map<VariableDescriptor, VariableControlFlowState> enterInstructionData,
            @NotNull LexicalScopeVariableInfo lexicalScopeVariableInfo
    ) {
        if (!(instruction instanceof WriteValueInstruction) && !(instruction instanceof VariableDeclarationInstruction)) {
            return enterInstructionData;
        }
        VariableDescriptor variable = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext);
        if (variable == null) {
            return enterInstructionData;
        }
        Map<VariableDescriptor, VariableControlFlowState> exitInstructionData = Maps.newHashMap(enterInstructionData);
        if (instruction instanceof WriteValueInstruction) {
            // if writing to already initialized object
            if (!PseudocodeUtil.isThisOrNoDispatchReceiver((WriteValueInstruction) instruction, bindingContext)) {
                return enterInstructionData;
            }

            VariableControlFlowState enterInitState = enterInstructionData.get(variable);
            VariableControlFlowState initializationAtThisElement =
                    VariableControlFlowState
                            .create(((WriteValueInstruction) instruction).getElement() instanceof KtProperty, enterInitState);
            exitInstructionData.put(variable, initializationAtThisElement);
        }
        else { // instruction instanceof VariableDeclarationInstruction
            VariableControlFlowState enterInitState = enterInstructionData.get(variable);
            if (enterInitState == null) {
                enterInitState = getDefaultValueForInitializers(variable, instruction, lexicalScopeVariableInfo);
            }
            if (enterInitState == null || !enterInitState.mayBeInitialized() || !enterInitState.isDeclared) {
                boolean isInitialized = enterInitState != null && enterInitState.mayBeInitialized();
                VariableControlFlowState variableDeclarationInfo = VariableControlFlowState.create(isInitialized, true);
                exitInstructionData.put(variable, variableDeclarationInfo);
            }
        }
        return exitInstructionData;
    }

// variable use

    @NotNull
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> getVariableUseStatusData() {
        return pseudocodeVariableDataCollector.collectData(
                BACKWARD, /*mergeDataWithLocalDeclarations=*/ true,
                new InstructionDataMergeStrategy<VariableUseState>() {
                    @NotNull
                    @Override
                    public Edges<Map<VariableDescriptor, VariableUseState>> invoke(
                            @NotNull Instruction instruction,
                            @NotNull Collection<? extends Map<VariableDescriptor, VariableUseState>> incomingEdgesData
                    ) {

                        Map<VariableDescriptor, VariableUseState> enterResult = Maps.newHashMap();
                        for (Map<VariableDescriptor, VariableUseState> edgeData : incomingEdgesData) {
                            for (Map.Entry<VariableDescriptor, VariableUseState> entry : edgeData.entrySet()) {
                                VariableDescriptor variableDescriptor = entry.getKey();
                                VariableUseState variableUseState = entry.getValue();
                                enterResult.put(variableDescriptor, variableUseState.merge(enterResult.get(variableDescriptor)));
                            }
                        }
                        VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(
                                instruction, true, bindingContext);
                        if (variableDescriptor == null ||
                            (!(instruction instanceof ReadValueInstruction) && !(instruction instanceof WriteValueInstruction))) {
                            return new Edges<Map<VariableDescriptor, VariableUseState>>(enterResult, enterResult);
                        }
                        Map<VariableDescriptor, VariableUseState> exitResult = Maps.newHashMap(enterResult);
                        if (instruction instanceof ReadValueInstruction) {
                            exitResult.put(variableDescriptor, VariableUseState.READ);
                        }
                        else { //instruction instanceof WriteValueInstruction
                            VariableUseState variableUseState = enterResult.get(variableDescriptor);
                            if (variableUseState == null) {
                                variableUseState = VariableUseState.UNUSED;
                            }
                            switch (variableUseState) {
                                case UNUSED:
                                case ONLY_WRITTEN_NEVER_READ:
                                    exitResult.put(variableDescriptor, VariableUseState.ONLY_WRITTEN_NEVER_READ);
                                    break;
                                case WRITTEN_AFTER_READ:
                                case READ:
                                    exitResult.put(variableDescriptor, VariableUseState.WRITTEN_AFTER_READ);
                            }
                        }
                        return new Edges<Map<VariableDescriptor, VariableUseState>>(enterResult, exitResult);
                    }
                }
        );
    }

    private enum TriInitState {
        INITIALIZED("I"), UNKNOWN("I?"), NOT_INITIALIZED("");

        private final String s;

        TriInitState(String s) {
            this.s = s;
        }

        private TriInitState merge(@NotNull TriInitState other) {
            if (this == other) return this;
            return UNKNOWN;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    public static class VariableControlFlowState {

        public final TriInitState initState;
        public final boolean isDeclared;

        private VariableControlFlowState(TriInitState initState, boolean isDeclared) {
            this.initState = initState;
            this.isDeclared = isDeclared;
        }

        private static final VariableControlFlowState VS_IT = new VariableControlFlowState(TriInitState.INITIALIZED, true);
        private static final VariableControlFlowState VS_IF = new VariableControlFlowState(TriInitState.INITIALIZED, false);
        private static final VariableControlFlowState VS_UT = new VariableControlFlowState(TriInitState.UNKNOWN, true);
        private static final VariableControlFlowState VS_UF = new VariableControlFlowState(TriInitState.UNKNOWN, false);
        private static final VariableControlFlowState VS_NT = new VariableControlFlowState(TriInitState.NOT_INITIALIZED, true);
        private static final VariableControlFlowState VS_NF = new VariableControlFlowState(TriInitState.NOT_INITIALIZED, false);


        private static VariableControlFlowState create(TriInitState initState, boolean isDeclared) {
            switch (initState) {
                case INITIALIZED: return isDeclared ? VS_IT : VS_IF;
                case UNKNOWN: return isDeclared ? VS_UT : VS_UF;
                default: return isDeclared ? VS_NT : VS_NF;
            }
        }

        private static VariableControlFlowState create(boolean isInitialized, boolean isDeclared) {
            return create(isInitialized ? TriInitState.INITIALIZED : TriInitState.NOT_INITIALIZED, isDeclared);
        }

        private static VariableControlFlowState create(boolean isInitialized) {
            return create(isInitialized, false);
        }

        private static VariableControlFlowState create(boolean isDeclaredHere, @Nullable VariableControlFlowState mergedEdgesData) {
            return create(true, isDeclaredHere || (mergedEdgesData != null && mergedEdgesData.isDeclared));
        }

        public boolean definitelyInitialized() {
            return initState == TriInitState.INITIALIZED;
        }

        public boolean mayBeInitialized() {
            return initState != TriInitState.NOT_INITIALIZED;
        }

        @Override
        public String toString() {
            if (initState == TriInitState.NOT_INITIALIZED && !isDeclared) return "-";
            return initState + (isDeclared ? "D" : "");
        }
    }

    public enum VariableUseState {
        READ(3),
        WRITTEN_AFTER_READ(2),
        ONLY_WRITTEN_NEVER_READ(1),
        UNUSED(0);

        private final int priority;

        VariableUseState(int priority) {
            this.priority = priority;
        }

        private VariableUseState merge(@Nullable VariableUseState variableUseState) {
            if (variableUseState == null || priority > variableUseState.priority) return this;
            return variableUseState;
        }

        public static boolean isUsed(@Nullable VariableUseState variableUseState) {
            return variableUseState != null && variableUseState != UNUSED;
        }
    }
}
