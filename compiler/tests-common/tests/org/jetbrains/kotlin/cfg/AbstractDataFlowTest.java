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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import javaslang.Tuple2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges;
import org.jetbrains.kotlin.cfg.variable.PseudocodeVariablesData;
import org.jetbrains.kotlin.cfg.variable.ReadOnlyInitVariableControlFlowInfo;
import org.jetbrains.kotlin.cfg.variable.VariableUseState;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataFlowTest extends AbstractPseudocodeTest {

    @Override
    public void dumpInstructions(
            @NotNull PseudocodeImpl pseudocode,
            @NotNull StringBuilder out,
            @NotNull BindingContext bindingContext
    ) {
        PseudocodeVariablesData pseudocodeVariablesData = new PseudocodeVariablesData(pseudocode.getRootPseudocode(), bindingContext);
        Map<Instruction, Edges<ReadOnlyInitVariableControlFlowInfo>> variableInitializers =
                pseudocodeVariablesData.getVariableInitializers();
        Map<Instruction, Edges<ReadOnlyControlFlowInfo<VariableDescriptor, VariableUseState>>> useStatusData =
                pseudocodeVariablesData.getVariableUseStatusData();
        String initPrefix = "    INIT:";
        String usePrefix = "    USE:";
        int initializersColumnWidth = countDataColumnWidth(initPrefix, pseudocode.getInstructionsIncludingDeadCode(), variableInitializers,
                                                           pseudocodeVariablesData);

        dumpInstructions(pseudocode, out, (instruction, next, prev) -> {
            StringBuilder result = new StringBuilder();
            Edges<ReadOnlyInitVariableControlFlowInfo> initializersEdges = variableInitializers.get(instruction);
            Edges<ReadOnlyInitVariableControlFlowInfo> previousInitializersEdges = variableInitializers.get(prev);
            String initializersData = "";
            if (initializersEdges != null && !initializersEdges.equals(previousInitializersEdges)) {
                initializersData = dumpEdgesData(initPrefix, initializersEdges, pseudocodeVariablesData);
            }
            result.append(String.format("%1$-" + initializersColumnWidth + "s", initializersData));

            Edges<ReadOnlyControlFlowInfo<VariableDescriptor, VariableUseState>> useStatusEdges = useStatusData.get(instruction);
            Edges<ReadOnlyControlFlowInfo<VariableDescriptor, VariableUseState>> nextUseStatusEdges = useStatusData.get(next);
            if (useStatusEdges != null && !useStatusEdges.equals(nextUseStatusEdges)) {
                result.append(dumpEdgesData(usePrefix, useStatusEdges, pseudocodeVariablesData));
            }
            return result.toString();
        });
    }

    private static int countDataColumnWidth(
            @NotNull String prefix,
            @NotNull List<Instruction> instructions,
            @NotNull Map<Instruction, Edges<ReadOnlyInitVariableControlFlowInfo>> data,
            @NotNull PseudocodeVariablesData variablesData
    ) {
        int maxWidth = 0;
        for (Instruction instruction : instructions) {
            Edges<ReadOnlyInitVariableControlFlowInfo> edges = data.get(instruction);
            if (edges == null) continue;
            int length = dumpEdgesData(prefix, edges, variablesData).length();
            if (maxWidth < length) {
                maxWidth = length;
            }
        }

        return maxWidth;
    }

    @NotNull
    private static <S, I extends ReadOnlyControlFlowInfo<VariableDescriptor, S>> String dumpEdgesData(
            String prefix,
            @NotNull Edges<I> edges,
            @NotNull PseudocodeVariablesData variablesData
    ) {
        return prefix +
               " in: " + renderVariableMap(edges.getIncoming().asMap(), variablesData) +
               " out: " + renderVariableMap(edges.getOutgoing().asMap(), variablesData);
    }

    private static <S> String renderVariableMap(
            javaslang.collection.Map<VariableDescriptor, S> map,
            PseudocodeVariablesData variablesData
    ) {
        List<String> result = Lists.newArrayList();
        for (Tuple2<VariableDescriptor, S> entry : map) {
            VariableDescriptor variable = entry._1;
            S state = entry._2;

            if (variablesData.isVariableWithTrivialInitializer(variable)) continue;

            result.add(variable.getName() + "=" + state);
        }
        Collections.sort(result);
        return "{" + StringUtil.join(result, ", ") + "}";
    }
}
