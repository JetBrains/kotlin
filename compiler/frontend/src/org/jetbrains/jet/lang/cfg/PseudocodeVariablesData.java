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

    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> variableInitializersMap;
    private Map<Instruction, Edges<Map<VariableDescriptor, VariableUseStatus>>> variableStatusMap;

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
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> getVariableInitializers() {
        if (variableInitializersMap == null) {
            variableInitializersMap = getVariableInitializers(pseudocode);
        }
        return variableInitializersMap;
    }

    @NotNull
    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> getVariableInitializers(@NotNull Pseudocode pseudocode) {

        Set<VariableDescriptor> usedVariables = getUsedVariables(pseudocode);
        Set<VariableDescriptor> declaredVariables = getDeclaredVariables(pseudocode);
        Map<VariableDescriptor, VariableInitializers> initialMap = Collections.emptyMap();
        final Map<VariableDescriptor, VariableInitializers> initialMapForStartInstruction = prepareInitializersMapForStartInstruction(
                usedVariables, declaredVariables);

        Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> variableInitializersMap = PseudocodeTraverser.collectData(
                pseudocode, /* directOrder = */ true, /* lookInside = */ false,
                initialMap, initialMapForStartInstruction, new PseudocodeTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableInitializers>>() {
            @Override
            public Edges<Map<VariableDescriptor, VariableInitializers>> execute(
                    @NotNull Instruction instruction, @NotNull Collection<Map<VariableDescriptor, VariableInitializers>> incomingEdgesData) {

                Map<VariableDescriptor, VariableInitializers> enterInstructionData = mergeIncomingEdgesDataForInitializers(incomingEdgesData);
                Map<VariableDescriptor, VariableInitializers> exitInstructionData =
                        addVariableInitializerFromCurrentInstructionIfAny(instruction, enterInstructionData);
                return Edges.create(enterInstructionData, exitInstructionData);
            }
        });


        for (LocalDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = localDeclarationInstruction.getBody();
            Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> initializersForLocalDeclaration = getVariableInitializers(localPseudocode);

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
    private Map<VariableDescriptor, VariableInitializers> prepareInitializersMapForStartInstruction(
            @NotNull Collection<VariableDescriptor> usedVariables,
            @NotNull Collection<VariableDescriptor> declaredVariables) {

        Map<VariableDescriptor, VariableInitializers> initialMapForStartInstruction = Maps.newHashMap();
        VariableInitializers    isInitializedForExternalVariable = new VariableInitializers(true);
        VariableInitializers isNotInitializedForDeclaredVariable = new VariableInitializers(false);

        for (VariableDescriptor variable : usedVariables) {
            if (declaredVariables.contains(variable)) {
                initialMapForStartInstruction.put(variable, isNotInitializedForDeclaredVariable);
            }
            else {
                initialMapForStartInstruction.put(variable, isInitializedForExternalVariable);
            }
        }
        return initialMapForStartInstruction;
    }

    @NotNull
    private Map<VariableDescriptor, VariableInitializers> mergeIncomingEdgesDataForInitializers(
            @NotNull Collection<Map<VariableDescriptor, VariableInitializers>> incomingEdgesData) {

        Set<VariableDescriptor> variablesInScope = Sets.newHashSet();
        for (Map<VariableDescriptor, VariableInitializers> edgeData : incomingEdgesData) {
            variablesInScope.addAll(edgeData.keySet());
        }

        Map<VariableDescriptor, VariableInitializers> enterInstructionData = Maps.newHashMap();
        for (VariableDescriptor variable : variablesInScope) {
            Set<VariableInitializers> edgesDataForVariable = Sets.newHashSet();
            for (Map<VariableDescriptor, VariableInitializers> edgeData : incomingEdgesData) {
                VariableInitializers initializers = edgeData.get(variable);
                if (initializers != null) {
                    edgesDataForVariable.add(initializers);
                }
            }
            enterInstructionData.put(variable, new VariableInitializers(edgesDataForVariable));
        }
        return enterInstructionData;
    }

    @NotNull
    private Map<VariableDescriptor, VariableInitializers> addVariableInitializerFromCurrentInstructionIfAny(
            @NotNull Instruction instruction, @NotNull Map<VariableDescriptor, VariableInitializers> enterInstructionData) {

        if (!(instruction instanceof WriteValueInstruction) && !(instruction instanceof VariableDeclarationInstruction)) {
            return enterInstructionData;
        }
        VariableDescriptor variable = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, false, bindingContext);
        if (variable == null) {
            return enterInstructionData;
        }
        Map<VariableDescriptor, VariableInitializers> exitInstructionData = Maps.newHashMap(enterInstructionData);
        if (instruction instanceof WriteValueInstruction) {
            VariableInitializers enterInitializers = enterInstructionData.get(variable);
            VariableInitializers initializationAtThisElement = new VariableInitializers(((WriteValueInstruction) instruction).getElement(), enterInitializers);
            exitInstructionData.put(variable, initializationAtThisElement);
        }
        else {
            VariableInitializers enterInitializers = enterInstructionData.get(variable);
            if (enterInitializers == null || !enterInitializers.isInitialized() || !enterInitializers.isDeclared()) {
                JetElement element = ((VariableDeclarationInstruction) instruction).getElement();
                if (element instanceof JetProperty) {
                    JetProperty property = (JetProperty) element;
                    if (property.getInitializer() == null) {
                        boolean isInitialized = enterInitializers != null && enterInitializers.isInitialized();
                        VariableInitializers variableDeclarationInfo = new VariableInitializers(isInitialized, true);
                        exitInstructionData.put(variable, variableDeclarationInfo);
                    }
                }
            }
        }
        return exitInstructionData;
    }

// variable use

    @NotNull
    public Map<Instruction, Edges<Map<VariableDescriptor, VariableUseStatus>>> getVariableUseStatusData() {
        if (variableStatusMap == null) {
            Map<VariableDescriptor, VariableUseStatus> sinkInstructionData = Maps.newHashMap();
            for (VariableDescriptor usedVariable : usedVariablesInEachDeclaration.get(pseudocode)) {
                sinkInstructionData.put(usedVariable, VariableUseStatus.UNUSED);
            }
            InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseStatus>> collectVariableUseStatusStrategy = new InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseStatus>>() {
                @Override
                public Edges<Map<VariableDescriptor, VariableUseStatus>> execute(@NotNull Instruction instruction,
                        @NotNull Collection<Map<VariableDescriptor, VariableUseStatus>> incomingEdgesData) {

                    Map<VariableDescriptor, VariableUseStatus> enterResult = Maps.newHashMap();
                    for (Map<VariableDescriptor, VariableUseStatus> edgeData : incomingEdgesData) {
                        for (Map.Entry<VariableDescriptor, VariableUseStatus> entry : edgeData.entrySet()) {
                            VariableDescriptor variableDescriptor = entry.getKey();
                            VariableUseStatus variableUseStatus = entry.getValue();
                            enterResult.put(variableDescriptor, variableUseStatus.merge(enterResult.get(variableDescriptor)));
                        }
                    }
                    VariableDescriptor variableDescriptor = PseudocodeUtil.extractVariableDescriptorIfAny(instruction, true,
                                                                                                          bindingContext);
                    if (variableDescriptor == null ||
                        (!(instruction instanceof ReadValueInstruction) && !(instruction instanceof WriteValueInstruction))) {
                        return Edges.create(enterResult, enterResult);
                    }
                    Map<VariableDescriptor, VariableUseStatus> exitResult = Maps.newHashMap(enterResult);
                    if (instruction instanceof ReadValueInstruction) {
                        exitResult.put(variableDescriptor, VariableUseStatus.LAST_READ);
                    }
                    else {
                        VariableUseStatus variableUseStatus = enterResult.get(variableDescriptor);
                        if (variableUseStatus == null) {
                            variableUseStatus = VariableUseStatus.UNUSED;
                        }
                        switch (variableUseStatus) {
                            case UNUSED:
                            case ONLY_WRITTEN_NEVER_READ:
                                exitResult.put(variableDescriptor, VariableUseStatus.ONLY_WRITTEN_NEVER_READ);
                                break;
                            case LAST_WRITTEN:
                            case LAST_READ:
                                exitResult.put(variableDescriptor, VariableUseStatus.LAST_WRITTEN);
                        }
                    }
                    return Edges.create(enterResult, exitResult);
                }
            };
            variableStatusMap = PseudocodeTraverser.collectData(pseudocode, false, true,
                                                                Collections.<VariableDescriptor, VariableUseStatus>emptyMap(),
                                                                sinkInstructionData, collectVariableUseStatusStrategy);
        }
        return variableStatusMap;
    }

    public static class VariableInitializers {
        private final Set<JetElement> possibleLocalInitializers = Sets.newHashSet();
        private boolean isInitialized;
        private boolean isDeclared;

        public VariableInitializers(boolean isInitialized) {
            this(isInitialized, false);
        }

        public VariableInitializers(boolean isInitialized, boolean isDeclared) {
            this.isInitialized = isInitialized;
            this.isDeclared = isDeclared;
        }

        public VariableInitializers(JetElement element, @Nullable VariableInitializers previous) {
            isInitialized = true;
            isDeclared = element instanceof JetProperty || (previous != null && previous.isDeclared());
            possibleLocalInitializers.add(element);
        }

        public VariableInitializers(Set<VariableInitializers> edgesData) {
            isInitialized = true;
            isDeclared = true;
            for (VariableInitializers edgeData : edgesData) {
                if (!edgeData.isInitialized) {
                    isInitialized = false;
                }
                if (!edgeData.isDeclared) {
                    isDeclared = false;
                }
                possibleLocalInitializers.addAll(edgeData.possibleLocalInitializers);
            }
        }

        public Set<JetElement> getPossibleLocalInitializers() {
            return possibleLocalInitializers;
        }

        public boolean isInitialized() {
            return isInitialized;
        }

        public boolean isDeclared() {
            return isDeclared;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VariableInitializers)) return false;

            VariableInitializers that = (VariableInitializers) o;

            if (isDeclared != that.isDeclared) return false;
            if (isInitialized != that.isInitialized) return false;
            if (possibleLocalInitializers != null
                ? !possibleLocalInitializers.equals(that.possibleLocalInitializers)
                : that.possibleLocalInitializers != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = possibleLocalInitializers != null ? possibleLocalInitializers.hashCode() : 0;
            result = 31 * result + (isInitialized ? 1 : 0);
            result = 31 * result + (isDeclared ? 1 : 0);
            return result;
        }
    }

    public static enum VariableUseStatus {
        LAST_READ(3),
        LAST_WRITTEN(2),
        ONLY_WRITTEN_NEVER_READ(1),
        UNUSED(0);

        private final int importance;

        VariableUseStatus(int importance) {
            this.importance = importance;
        }

        public VariableUseStatus merge(@Nullable VariableUseStatus variableUseStatus) {
            if (variableUseStatus == null || importance > variableUseStatus.importance) return this;
            return variableUseStatus;
        }
    }
}
