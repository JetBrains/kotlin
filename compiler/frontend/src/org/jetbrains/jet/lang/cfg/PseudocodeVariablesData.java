/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author svtk
 */
public class PseudocodeVariablesData {
    private final Pseudocode pseudocode;
    private final BindingContext bindingContext;

    private final Map<Pseudocode, Set<VariableDescriptor>> declaredVariablesInEachDeclaration = Maps.newHashMap();
    private final Map<Pseudocode, Set<VariableDescriptor>> usedVariablesInEachDeclaration = Maps.newHashMap();

    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> variableInitializersMap;
    private Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> variableStatusMap;

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
        Set<VariableDescriptor> usedVariables = usedVariablesInEachDeclaration.get(pseudocode);
        if (usedVariables == null) {
            final Set<VariableDescriptor> result = Sets.newHashSet();
            PseudocodeTraverser.traverse(pseudocode, true, new InstructionAnalyzeStrategy() {
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
            usedVariablesInEachDeclaration.put(pseudocode, usedVariables);
        }
        return usedVariables;
    }

    @NotNull
    public Set<VariableDescriptor> getDeclaredVariables(@NotNull Pseudocode pseudocode) {
        Set<VariableDescriptor> declaredVariables = declaredVariablesInEachDeclaration.get(pseudocode);
        if (declaredVariables == null) {
            declaredVariables = Sets.newHashSet();
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
            declaredVariables = Collections.unmodifiableSet(declaredVariables);
            declaredVariablesInEachDeclaration.put(pseudocode, declaredVariables);
        }
        return declaredVariables;
    }

// variable initializers

    @NotNull
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> getVariableInitializers() {
        if (variableInitializersMap == null) {
            variableInitializersMap = getVariableInitializers(pseudocode);
        }
        return variableInitializersMap;
    }

    @NotNull
    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> getVariableInitializers(@NotNull Pseudocode pseudocode) {

        Set<VariableDescriptor> usedVariables = getUsedVariables(pseudocode);
        Set<VariableDescriptor> declaredVariables = getDeclaredVariables(pseudocode);
        Map<VariableDescriptor, VariableInitState> initialMap = Collections.emptyMap();
        final Map<VariableDescriptor, VariableInitState> initialMapForStartInstruction = prepareInitializersMapForStartInstruction(
                usedVariables, declaredVariables);

        Map<Instruction, Edges<Map<VariableDescriptor, VariableInitState>>> variableInitializersMap = PseudocodeTraverser.collectData(
                pseudocode, /* directOrder = */ true, /* lookInside = */ false,
                initialMap, initialMapForStartInstruction, new PseudocodeTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableInitState>>() {
            @Override
            public Edges<Map<VariableDescriptor, VariableInitState>> execute(
                    @NotNull Instruction instruction, @NotNull Collection<Map<VariableDescriptor, VariableInitState>> incomingEdgesData) {

                Map<VariableDescriptor, VariableInitState> enterInstructionData = mergeIncomingEdgesDataForInitializers(incomingEdgesData);
                Map<VariableDescriptor, VariableInitState> exitInstructionData =
                        addVariableInitializerFromCurrentInstructionIfAny(instruction, enterInstructionData);
                return Edges.create(enterInstructionData, exitInstructionData);
            }
        });


        for (LocalDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = localDeclarationInstruction.getBody();
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
    private Map<VariableDescriptor, VariableInitState> prepareInitializersMapForStartInstruction(
            @NotNull Collection<VariableDescriptor> usedVariables,
            @NotNull Collection<VariableDescriptor> declaredVariables) {

        Map<VariableDescriptor, VariableInitState> initialMapForStartInstruction = Maps.newHashMap();
        VariableInitState initializedForExternalVariable = new VariableInitState(true);
        VariableInitState notInitializedForDeclaredVariable = new VariableInitState(false);

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
    private Map<VariableDescriptor, VariableInitState> mergeIncomingEdgesDataForInitializers(
            @NotNull Collection<Map<VariableDescriptor, VariableInitState>> incomingEdgesData) {

        Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
        for (Map<VariableDescriptor, VariableInitState> edgeData : incomingEdgesData) {
            variablesInScope.addAll(edgeData.keySet());
        }

        Map<VariableDescriptor, VariableInitState> enterInstructionData = Maps.newHashMap();
        for (VariableDescriptor variable : variablesInScope) {
            Set<VariableInitState> edgesDataForVariable = Sets.newHashSet();
            for (Map<VariableDescriptor, VariableInitState> edgeData : incomingEdgesData) {
                VariableInitState initState = edgeData.get(variable);
                if (initState != null) {
                    edgesDataForVariable.add(initState);
                }
            }
            enterInstructionData.put(variable, new VariableInitState(edgesDataForVariable));
        }
        return enterInstructionData;
    }

    @NotNull
    private Map<VariableDescriptor, VariableInitState> addVariableInitializerFromCurrentInstructionIfAny(
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
                    new VariableInitState(((WriteValueInstruction) instruction).getElement() instanceof JetProperty, enterInitState);
            exitInstructionData.put(variable, initializationAtThisElement);
        }
        else { // instruction instanceof VariableDeclarationInstruction
            VariableInitState enterInitState = enterInstructionData.get(variable);
            if (enterInitState == null || !enterInitState.isInitialized || !enterInitState.isDeclared) {
                JetElement element = ((VariableDeclarationInstruction) instruction).getElement();
                if (element instanceof JetProperty) {
                    JetProperty property = (JetProperty) element;
                    if (property.getInitializer() == null) {
                        boolean isInitialized = enterInitState != null && enterInitState.isInitialized;
                        VariableInitState variableDeclarationInfo = new VariableInitState(isInitialized, true);
                        exitInstructionData.put(variable, variableDeclarationInfo);
                    }
                }
            }
        }
        return exitInstructionData;
    }

// variable use

    @NotNull
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableUseState>>> getVariableUseStatusData() {
        if (variableStatusMap == null) {
            Map<VariableDescriptor, VariableUseState> sinkInstructionData = Maps.newHashMap();
            for (VariableDescriptor usedVariable : usedVariablesInEachDeclaration.get(pseudocode)) {
                sinkInstructionData.put(usedVariable, VariableUseState.UNUSED);
            }
            InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseState>> collectVariableUseStatusStrategy = new InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseState>>() {
                @Override
                public Edges<Map<VariableDescriptor, VariableUseState>> execute(@NotNull Instruction instruction,
                        @NotNull Collection<Map<VariableDescriptor, VariableUseState>> incomingEdgesData) {

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
            variableStatusMap = PseudocodeTraverser.collectData(pseudocode, false, true,
                                                                Collections.<VariableDescriptor, VariableUseState>emptyMap(),
                                                                sinkInstructionData, collectVariableUseStatusStrategy);
        }
        return variableStatusMap;
    }

    public static class VariableInitState {
        public final boolean isInitialized;
        public final boolean isDeclared;

        public VariableInitState(boolean isInitialized) {
            this(isInitialized, false);
        }

        public VariableInitState(boolean isInitialized, boolean isDeclared) {
            this.isInitialized = isInitialized;
            this.isDeclared = isDeclared;
        }

        public VariableInitState(boolean isDeclaredHere, @Nullable VariableInitState mergedEdgesData) {
            isInitialized = true;
            isDeclared = isDeclaredHere || (mergedEdgesData != null && mergedEdgesData.isDeclared);
        }

        public VariableInitState(@NotNull Set<VariableInitState> edgesData) {
            boolean isInitialized = true;
            boolean isDeclared = true;
            for (VariableInitState edgeData : edgesData) {
                if (!edgeData.isInitialized) {
                    isInitialized = false;
                }
                if (!edgeData.isDeclared) {
                    isDeclared = false;
                }
            }
            this.isInitialized = isInitialized;
            this.isDeclared = isDeclared;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VariableInitState)) return false;

            VariableInitState that = (VariableInitState) o;

            if (isDeclared != that.isDeclared) return false;
            if (isInitialized != that.isInitialized) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (isInitialized ? 1 : 0);
            result = 31 * result + (isDeclared ? 1 : 0);
            return result;
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

        public VariableUseState merge(@Nullable VariableUseState variableUseState) {
            if (variableUseState == null || importance > variableUseState.importance) return this;
            return variableUseState;
        }
    }
}
