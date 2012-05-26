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

import com.google.common.collect.Maps;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.Instruction;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;

import java.util.Map;

/**
 * @author svtk
 */
public class InstructionData {
    public final Instruction instruction;
    public final PseudocodeData pseudocodeData;

    private final Map<VariableDescriptor, Edges<VariableInitializers>> initializersMap = Maps.newHashMap();
    private final Map<VariableDescriptor, Edges<VariableUseStatus>> useStatusMap = Maps.newHashMap();

    public InstructionData(@NotNull PseudocodeData data, @NotNull Instruction instruction,
            @NotNull Edges<Map<VariableDescriptor, VariableInitializers>> pairOfVariableInitializersMap,
            @NotNull Edges<Map<VariableDescriptor, VariableUseStatus>> pairOfVariableStatusMap) {
        pseudocodeData = data;
        this.instruction = instruction;

        for (Map.Entry<VariableDescriptor, VariableInitializers> entry : pairOfVariableInitializersMap.out.entrySet()) {
            VariableDescriptor variableDescriptor = entry.getKey();
            VariableInitializers in = pairOfVariableInitializersMap.in.get(variableDescriptor);
            VariableInitializers out = entry.getValue();
            initializersMap.put(variableDescriptor, Edges.create(in, out));
        }

        for (Map.Entry<VariableDescriptor, VariableUseStatus> entry : pairOfVariableStatusMap.out.entrySet()) {
            VariableDescriptor variableDescriptor = entry.getKey();
            VariableUseStatus in = pairOfVariableStatusMap.in.get(variableDescriptor);
            VariableUseStatus out = entry.getValue();
            if (in == null || out == null) continue;
            useStatusMap.put(variableDescriptor, Edges.create(in, out));
        }
    }

    @NotNull
    public Map<VariableDescriptor, Edges<VariableInitializers>> getInitializersMap() {
        return initializersMap;
    }

    @NotNull
    public Map<VariableDescriptor, Edges<VariableUseStatus>> getUseStatusMap() {
        return useStatusMap;
    }
}
