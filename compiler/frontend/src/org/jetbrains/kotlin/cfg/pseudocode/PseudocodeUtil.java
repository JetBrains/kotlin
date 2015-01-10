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

package org.jetbrains.kotlin.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.JetControlFlowProcessor;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.ReadValueInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.WriteValueInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;

public class PseudocodeUtil {
    @NotNull
    public static Pseudocode generatePseudocode(@NotNull JetDeclaration declaration, @NotNull final BindingContext bindingContext) {
        BindingTrace mockTrace = new BindingTrace() {
            @NotNull
            @Override
            public BindingContext getBindingContext() {
                return bindingContext;
            }

            @Override
            public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            }

            @Override
            public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            }

            @Override
            public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                return bindingContext.get(slice, key);
            }

            @NotNull
            @Override
            public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                return bindingContext.getKeys(slice);
            }

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
            }
        };
        return new JetControlFlowProcessor(mockTrace).generatePseudocode(declaration);
    }

    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull Instruction instruction, boolean onlyReference, @NotNull BindingContext bindingContext) {
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
