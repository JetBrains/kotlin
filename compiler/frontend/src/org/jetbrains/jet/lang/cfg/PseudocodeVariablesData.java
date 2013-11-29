/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser.*;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS;
import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.LookInsideStrategy.SKIP_LOCAL_DECLARATIONS;
import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.TraversalOrder.BACKWARD;
import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.TraversalOrder.FORWARD;

public class PseudocodeVariablesData {
    private final Pseudocode pseudocode;
    private final BindingContext bindingContext;

    private final Map<Pseudocode, Set<VariableDescriptor>> declaredVariablesForDeclaration = Maps.newHashMap();
    private final Map<Pseudocode, Set<VariableDescriptor>> usedVariablesForDeclaration = Maps.newHashMap();

    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> variableInitializers;

    public PseudocodeVariablesData(@NotNull Pseudocode pseudocode, @NotNull BindingContext bindingContext) {
        this.pseudocode = pseudocode;
        this.bindingContext = bindingContext;
    }

    @NotNull
    public Pseudocode getPseudocode() {
        return pseudocode;
    }

    @NotNull
    public Set<VariableDescriptor> getUsedVariables(@NotNull Pseudocode pseudocode) {
        Set<VariableDescriptor> usedVariables = usedVariablesForDeclaration.get(pseudocode);
        if (usedVariables == null) {
            final Set<VariableDescriptor> result = Sets.newHashSet();
            PseudocodeTraverser.traverse(pseudocode, FORWARD, new InstructionAnalyzeStrategy() {
                @Override
                public void execute(@NotNull Instruction instruction) {
                    VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false,
                                                                                                          bindingContext);
                    if (variableDescriptor != null) {
                        result.add(variableDescriptor);
                    }
                }
            });
            usedVariables = Collections.unmodifiableSet(result);
            usedVariablesForDeclaration.put(pseudocode, usedVariables);
        }
        return usedVariables;
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
                JetDeclaration variableDeclarationElement = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
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
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> getVariableInitializers() {
        if (variableInitializers == null) {
            variableInitializers = getVariableInitializers(pseudocode);
        }

        return variableInitializers;
    }

    @NotNull
    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> getVariableInitializers(@NotNull Pseudocode pseudocode) {

        Set<VariableDescriptor> usedVariables = getUsedVariables(pseudocode);
        Set<VariableDescriptor> declaredVariables = getDeclaredVariables(pseudocode, false);
        Map<VariableDescriptor, VariableInitState> initialMap = Collections.emptyMap();
        Map<VariableDescriptor, VariableInitState> initialMapForStartInstruction = prepareInitializersMapForStartInstruction(
                usedVariables, declaredVariables);

        Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> variableInitializersMap = PseudocodeTraverser.collectData(
                pseudocode, FORWARD, SKIP_LOCAL_DECLARATIONS,
                initialMap, initialMapForStartInstruction, new PseudocodeTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableInitState>>() {
            @Override
            public Edges<Map<VariableDescriptor, VariableInitState>> execute(
                    @NotNull Instruction instruction, @NotNull Collection<Map<VariableDescriptor, VariableInitState>> incomingEdgesData) {

                Map<VariableDescriptor, VariableInitState> enterInstructionData = mergeIncomingEdgesDataForInitializers(incomingEdgesData);
                Map<VariableDescriptor, VariableInitState> exitInstructionData =
                        addVariableInitStateFromCurrentInstructionIfAny(instruction, enterInstructionData);
                return Edges.create(enterInstructionData, exitInstructionData);
            }
        });


        for (LocalFunctionDeclarationInstruction localFunctionDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = localFunctionDeclarationInstruction.getBody();
            Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> initializersForLocalDeclaration = getVariableInitializers(localPseudocode);

            for (Instruction instruction : initializersForLocalDeclaration.keySet()) {
                //todo
                if (!variableInitializersMap.containsKey(instruction)) {
                    variableInitializersMap.put(instruction, initializersForLocalDeclaration.get(instruction));
                }
            }
            variableInitializersMap.putAll(initializersForLocalDeclaration);
        }
        return variableInitializersMap;
    }

    @NotNull
    private static Map<VariableDescriptor, VariableInitState> prepareInitializersMapForStartInstruction(
            @NotNull Collection<VariableDescriptor> usedVariables,
            @NotNull Collection<VariableDescriptor> declaredVariables) {

        Map<VariableDescriptor, VariableInitState> initialMapForStartInstruction = Maps.newHashMap();
        VariableInitState initializedForExternalVariable = VariableInitState.create(true);
        VariableInitState notInitializedForDeclaredVariable = VariableInitState.create(false);

        for (VariableDescriptor variable : usedVariables) {
            if (declaredVariables.contains(variable)) {
                initialMapForStartInstruction.put(variable, notInitializedForDeclaredVariable);
            }
            else {
                initialMapForStartInstruction.put(variable, initializedForExternalVariable);
            }
        }
        return initialMapForStartInstruction;
    }

    @NotNull
    private static Map<VariableDescriptor, VariableInitState> mergeIncomingEdgesDataForInitializers(
            @NotNull Collection<Map<VariableDescriptor, VariableInitState>> incomingEdgesData) {

        Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
        for (Map<VariableDescriptor, VariableInitState> edgeData : incomingEdgesData) {
            variablesInScope.addAll(edgeData.keySet());
        }

        Map<VariableDescriptor, VariableInitState> enterInstructionData = Maps.newHashMap();
        for (VariableDescriptor variable : variablesInScope) {
            boolean isInitialized = true;
            boolean isDeclared = true;
            for (Map<VariableDescriptor, VariableInitState> edgeData : incomingEdgesData) {
                VariableInitState initState = edgeData.get(variable);
                if (initState != null) {
                    if (!initState.isInitialized) {
                        isInitialized = false;
                    }
                    if (!initState.isDeclared) {
                        isDeclared = false;
                    }
                }
            }
            enterInstructionData.put(variable, VariableInitState.create(isInitialized, isDeclared));
        }
        return enterInstructionData;
    }

    @NotNull
    private Map<VariableDescriptor, VariableInitState> addVariableInitStateFromCurrentInstructionIfAny(
            @NotNull Instruction instruction, @NotNull Map<VariableDescriptor, VariableInitState> enterInstructionData) {

        if (!(instruction instanceof WriteValueInstruction) && !(instruction instanceof VariableDeclarationInstruction)) {
            return enterInstructionData;
        }
        VariableDescriptor variable = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext);
        if (variable == null) {
            return enterInstructionData;
        }
        Map<VariableDescriptor, VariableInitState> exitInstructionData = Maps.newHashMap(enterInstructionData);
        if (instruction instanceof WriteValueInstruction) {
            VariableInitState enterInitState = enterInstructionData.get(variable);
            VariableInitState initializationAtThisElement =
                    VariableInitState.create(((WriteValueInstruction) instruction).getElement() instanceof JetProperty, enterInitState);
            exitInstructionData.put(variable, initializationAtThisElement);
        }
        else { // instruction instanceof VariableDeclarationInstruction
            VariableInitState enterInitState = enterInstructionData.get(variable);
            if (enterInitState == null || !enterInitState.isInitialized || !enterInitState.isDeclared) {
                boolean isInitialized = enterInitState != null && enterInitState.isInitialized;
                VariableInitState variableDeclarationInfo = VariableInitState.create(isInitialized, true);
                exitInstructionData.put(variable, variableDeclarationInfo);
            }
        }
        return exitInstructionData;
    }

// variable use

    @NotNull
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> getVariableUseStatusData() {
        Map<VariableDescriptor, VariableUseState> sinkInstructionData = Maps.newHashMap();
        for (VariableDescriptor usedVariable : getUsedVariables(pseudocode)) {
            sinkInstructionData.put(usedVariable, VariableUseState.UNUSED);
        }
        InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseState>> collectVariableUseStatusStrategy =
                new InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseState>>() {
                    @Override
                    public Edges<Map<VariableDescriptor, VariableUseState>> execute(
                            @NotNull Instruction instruction,
                            @NotNull Collection<Map<VariableDescriptor, VariableUseState>> incomingEdgesData
                    ) {

                        Map<VariableDescriptor, VariableUseState> enterResult = Maps.newHashMap();
                        for (Map<VariableDescriptor, VariableUseState> edgeData : incomingEdgesData) {
                            for (Map.Entry<VariableDescriptor, VariableUseState> entry : edgeData.entrySet()) {
                                VariableDescriptor variableDescriptor = entry.getKey();
                                VariableUseState variableUseState = entry.getValue();
                                enterResult.put(variableDescriptor, variableUseState.merge(enterResult.get(variableDescriptor)));
                            }
                        }
                        VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true,
                                                                                                              bindingContext);
                        if (variableDescriptor == null ||
                            (!(instruction instanceof ReadValueInstruction) && !(instruction instanceof WriteValueInstruction))) {
                            return Edges.create(enterResult, enterResult);
                        }
                        Map<VariableDescriptor, VariableUseState> exitResult = Maps.newHashMap(enterResult);
                        if (instruction instanceof ReadValueInstruction) {
                            exitResult.put(variableDescriptor, VariableUseState.LAST_READ);
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
                                case LAST_WRITTEN:
                                case LAST_READ:
                                    exitResult.put(variableDescriptor, VariableUseState.LAST_WRITTEN);
                            }
                        }
                        return Edges.create(enterResult, exitResult);
                    }
                };
        return PseudocodeTraverser.collectData(pseudocode, BACKWARD, ANALYSE_LOCAL_DECLARATIONS,
                                               Collections.<VariableDescriptor, VariableUseState>emptyMap(),
                                               sinkInstructionData, collectVariableUseStatusStrategy);
    }

    public static class VariableInitState {
        public final boolean isInitialized;
        public final boolean isDeclared;

        private VariableInitState(boolean isInitialized, boolean isDeclared) {
            this.isInitialized = isInitialized;
            this.isDeclared = isDeclared;
        }

        private static final VariableInitState VS_TT = new VariableInitState(true, true);
        private static final VariableInitState VS_TF = new VariableInitState(true, false);
        private static final VariableInitState VS_FT = new VariableInitState(false, true);
        private static final VariableInitState VS_FF = new VariableInitState(false, false);


        private static VariableInitState create(boolean isInitialized, boolean isDeclared) {
            if (isInitialized) {
                if (isDeclared) return VS_TT;
                return VS_TF;
            }
            if (isDeclared) return VS_FT;
            return VS_FF;
        }

        private static VariableInitState create(boolean isInitialized) {
            return create(isInitialized, false);
        }

        private static VariableInitState create(boolean isDeclaredHere, @Nullable VariableInitState mergedEdgesData) {
            return create(true, isDeclaredHere || (mergedEdgesData != null && mergedEdgesData.isDeclared));
        }
    }

    public static enum VariableUseState {
        LAST_READ(3),
        LAST_WRITTEN(2),
        ONLY_WRITTEN_NEVER_READ(1),
        UNUSED(0);

        private final int importance;

        VariableUseState(int importance) {
            this.importance = importance;
        }

        private VariableUseState merge(@Nullable VariableUseState variableUseState) {
            if (variableUseState == null || importance > variableUseState.importance) return this;
            return variableUseState;
        }

        public static boolean isUsed(@Nullable VariableUseState variableUseState) {
            return variableUseState != null && variableUseState != UNUSED;
        }
    }
}
