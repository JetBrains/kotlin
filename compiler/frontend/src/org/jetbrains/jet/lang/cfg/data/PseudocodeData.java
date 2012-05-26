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

package org.jetbrains.jet.lang.cfg.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;

import java.util.*;

/**
 * @author svtk
 */
public class PseudocodeData {
    private final Pseudocode pseudocode;
    private final Map<Instruction, InstructionData> instructionDataMap = Maps.newLinkedHashMap();
    private final Map<Pseudocode, DeclarationData> declarationDataMap = Maps.newLinkedHashMap();

    //private final Map<Pseudocode, Set<VariableDescriptor>> declaredVariablesInEachDeclaration;
    //private final Map<Pseudocode, Set<VariableDescriptor>> usedVariablesInEachDeclaration;

    private final BindingContext bindingContext;

    public PseudocodeData(@NotNull Pseudocode pseudocode, @NotNull BindingContext bindingContext) {
        this.pseudocode = pseudocode;
        this.bindingContext = bindingContext;
        collectDeclarationData(pseudocode);

        DeclarationData declarationData = declarationDataMap.get(pseudocode);

        final Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> variableInitializersMap =
                collectVariableInitializers(pseudocode, declarationData);
        final Map<Instruction, Edges<Map<VariableDescriptor, VariableUseStatus>>> variableStatusMap =
                collectVariableStatusData();


        PseudocodeTraverser.traverseAndAnalyzeInstructionGraph(pseudocode, true,
                                                               new PseudocodeTraverser.SimpleInstructionDataAnalyzeStrategy() {
                                                                   @Override
                                                                   public void execute(@NotNull Instruction instruction) {
                                                                       instructionDataMap.put(instruction,
                                                                                              new InstructionData(PseudocodeData.this,
                                                                                                                  instruction,
                                                                                                                  variableInitializersMap
                                                                                                                          .get(instruction),
                                                                                                                  variableStatusMap
                                                                                                                          .get(instruction)));
                                                                   }
                                                               });
    }

    @NotNull
    public Pseudocode getPseudocode() {
        return pseudocode;
    }

    @NotNull
    public Map<Instruction, InstructionData> getInstructionDataMap() {
        return instructionDataMap;
    }

    @NotNull
    public DeclarationData getDeclarationData(Pseudocode pseudocode) {
        return declarationDataMap.get(pseudocode);
    }

    @NotNull
    public InstructionData getResultInfo(Pseudocode pseudocode) {
        return instructionDataMap.get(pseudocode.getExitInstruction());
    }

    public void traverseInstructionsGraph(boolean lookInside, boolean straightDirection,
            @NotNull TraverseInstructionGraphStrategy traverseInstructionGraphStrategy) {
        traverseInstructionsGraph(pseudocode, lookInside, straightDirection, traverseInstructionGraphStrategy);
    }

    private void traverseInstructionsGraph(@NotNull Pseudocode pseudocode,
            boolean lookInside,
            boolean straightDirection,
            @NotNull TraverseInstructionGraphStrategy traverseInstructionGraphStrategy) {
        List<Instruction> instructions = pseudocode.getInstructions();
        if (!straightDirection) {
            instructions = Lists.newArrayList(instructions);
            Collections.reverse(instructions);
        }
        for (Instruction instruction : instructions) {
            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                traverseInstructionsGraph(((LocalDeclarationInstruction) instruction).getBody(), lookInside, straightDirection,
                                          traverseInstructionGraphStrategy
                );
            }
            traverseInstructionGraphStrategy.execute(instruction, declarationDataMap.get(pseudocode), instructionDataMap.get(instruction));
        }
    }

    public interface TraverseInstructionGraphStrategy {
        void execute(@NotNull Instruction instruction, @NotNull DeclarationData declarationData, @NotNull InstructionData instructionData);
    }

    private void collectDeclarationData(Pseudocode pseudocode) {
        DeclarationData declarationData = new DeclarationData(pseudocode.getCorrespondingElement(), this, collectDeclaredVariables(pseudocode), collectUsedVariables(pseudocode));
        declarationDataMap.put(pseudocode, declarationData);

        for (LocalDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            collectDeclarationData(localDeclarationInstruction.getBody());
        }
    }

    private Set<VariableDescriptor> collectUsedVariables(@NotNull Pseudocode pseudocode) {
        final Set<VariableDescriptor> usedVariables = Sets.newHashSet();
        PseudocodeTraverser.traverseAndAnalyzeInstructionGraph(pseudocode, true,
                                                               new PseudocodeTraverser.SimpleInstructionDataAnalyzeStrategy() {
                                                                   @Override
                                                                   public void execute(@NotNull Instruction instruction) {
                                                                       VariableDescriptor variableDescriptor =
                                                                               extractVariableDescriptorIfAny(instruction, false);
                                                                       if (variableDescriptor != null) {
                                                                           usedVariables.add(variableDescriptor);
                                                                       }
                                                                   }
                                                               });
        return usedVariables;
    }

    private Set<VariableDescriptor> collectDeclaredVariables(@NotNull Pseudocode pseudocode) {
        final Set<VariableDescriptor> declaredVariables = Sets.newHashSet();
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
        return declaredVariables;
    }

// variable initializers

    private Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> collectVariableInitializers(
            Pseudocode pseudocode, DeclarationData data) {
        final Map<VariableDescriptor, VariableInitializers> initialMapForStartInstruction = prepareInitialMapForStartInstruction(data.usedVariables, data.declaredVariables);

        Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> result =
        PseudocodeTraverser.collectInformationFromInstructionGraph(false, true, pseudocode, Collections.<VariableDescriptor, VariableInitializers>emptyMap(), initialMapForStartInstruction,
                                                         new PseudocodeTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableInitializers>>() {
            @Override
            public Edges<Map<VariableDescriptor, VariableInitializers>> execute(
                    @NotNull Instruction instruction,
                    @NotNull Collection<Map<VariableDescriptor, VariableInitializers>> incomingEdgesData) {

                Map<VariableDescriptor, VariableInitializers> enterInstructionData = mergeIncomingEdgesData(incomingEdgesData);
                Map<VariableDescriptor, VariableInitializers> exitInstructionData = addVariableInitializerFromCurrentInstructionIfAny(instruction, enterInstructionData);
                return Edges.create(enterInstructionData, exitInstructionData);
            }
        });


        for (LocalDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = localDeclarationInstruction.getBody();
            Map<Instruction, Edges<Map<VariableDescriptor, VariableInitializers>>> initializersForLocalDeclaration =
                    collectVariableInitializers(localPseudocode, declarationDataMap.get(localPseudocode));

            for (Instruction instruction : initializersForLocalDeclaration.keySet()) {
                //todo
                if (!result.containsKey(instruction)) {
                    result.put(instruction, initializersForLocalDeclaration.get(instruction));
                }
            }
            result.putAll(initializersForLocalDeclaration);
        }

        return result;
    }

    private Map<VariableDescriptor, VariableInitializers> prepareInitialMapForStartInstruction(Collection<VariableDescriptor> usedVariables, Collection<VariableDescriptor> declaredVariables) {
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

    private Map<VariableDescriptor, VariableInitializers> mergeIncomingEdgesData(Collection<Map<VariableDescriptor, VariableInitializers>> incomingEdgesData) {
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

    private Map<VariableDescriptor, VariableInitializers> addVariableInitializerFromCurrentInstructionIfAny(Instruction instruction, Map<VariableDescriptor, VariableInitializers> enterInstructionData) {
        if (!(instruction instanceof WriteValueInstruction) && !(instruction instanceof VariableDeclarationInstruction)) {
            return enterInstructionData;
        }
        VariableDescriptor variable = extractVariableDescriptorIfAny(instruction, false);
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

    private Map<Instruction, Edges<Map<VariableDescriptor, VariableUseStatus>>> collectVariableStatusData() {
        Map<VariableDescriptor, VariableUseStatus> sinkInstructionData = Maps.newHashMap();
        for (VariableDescriptor usedVariable : declarationDataMap.get(pseudocode).usedVariables) {
            sinkInstructionData.put(usedVariable, VariableUseStatus.UNUSED);
        }
        return PseudocodeTraverser.collectInformationFromInstructionGraph(true, false, pseudocode, Collections.<VariableDescriptor, VariableUseStatus>emptyMap(), sinkInstructionData,
                                                         new PseudocodeTraverser.InstructionDataMergeStrategy<Map<VariableDescriptor, VariableUseStatus>>() {
            @Override
            public Edges<Map<VariableDescriptor, VariableUseStatus>> execute(@NotNull Instruction instruction, @NotNull Collection<Map<VariableDescriptor, VariableUseStatus>> incomingEdgesData) {
                Map<VariableDescriptor, VariableUseStatus> enterResult = Maps.newHashMap();
                for (Map<VariableDescriptor, VariableUseStatus> edgeData : incomingEdgesData) {
                    for (Map.Entry<VariableDescriptor, VariableUseStatus> entry : edgeData.entrySet()) {
                        VariableDescriptor variableDescriptor = entry.getKey();
                        VariableUseStatus variableUseStatus = entry.getValue();
                        enterResult.put(variableDescriptor, variableUseStatus.merge(enterResult.get(variableDescriptor)));
                    }
                }
                VariableDescriptor variableDescriptor = extractVariableDescriptorIfAny(instruction, true);
                if (variableDescriptor == null || (!(instruction instanceof ReadValueInstruction) && !(instruction instanceof WriteValueInstruction))) {
                    return Edges.create(enterResult, enterResult);
                }
                Map<VariableDescriptor, VariableUseStatus> exitResult = Maps.newHashMap(enterResult);
                if (instruction instanceof ReadValueInstruction) {
                    exitResult.put(variableDescriptor, VariableUseStatus.LAST_READ);
                }
                else {
                    VariableUseStatus variableUseStatus = enterResult.get(variableDescriptor);
                    if (variableUseStatus == null) variableUseStatus = VariableUseStatus.UNUSED;
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
        });
    }

//  Util methods

    @Nullable
    public VariableDescriptor extractVariableDescriptorIfAny(@NotNull Instruction instruction, boolean onlyReference) {
        JetElement element = null;
        if (instruction instanceof ReadValueInstruction) {
            element = ((ReadValueInstruction) instruction).getElement();
        }
        else if (instruction instanceof WriteValueInstruction) {
            element = ((WriteValueInstruction) instruction).getlValue();
        }
        else if (instruction instanceof VariableDeclarationInstruction) {
            element = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
        }
        return BindingContextUtils.extractVariableDescriptorIfAny(bindingContext, element, onlyReference);
    }

}
