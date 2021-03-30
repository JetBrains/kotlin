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

package org.jetbrains.kotlin.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.ControlFlowProcessor;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessTarget;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessValueInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.ResolvedCallUtilKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collection;

import static org.jetbrains.kotlin.resolve.BindingContextUtils.variableDescriptorForDeclaration;

public class PseudocodeUtil {
    @NotNull
    public static Pseudocode generatePseudocode(@NotNull KtDeclaration declaration, @NotNull BindingContext bindingContext) {
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

            @Nullable
            @Override
            public KotlinType getType(@NotNull KtExpression expression) {
                return bindingContext.getType(expression);
            }

            @Override
            public void recordType(@NotNull KtExpression expression, @Nullable KotlinType type) {
            }

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
            }

            @Override
            public boolean wantsDiagnostics() {
                return false;
            }
        };
        return new ControlFlowProcessor(mockTrace, null).generatePseudocode(declaration);
    }

    @Nullable
    public static VariableDescriptor extractVariableDescriptorFromReference(
            @NotNull Instruction instruction,
            @NotNull BindingContext bindingContext
    ) {
        if (instruction instanceof AccessValueInstruction) {
            KtElement element = ((AccessValueInstruction) instruction).getElement();
            if (element instanceof KtDeclaration) return null;
            VariableDescriptor descriptor = extractVariableDescriptorIfAny(instruction, bindingContext);
            if (descriptor instanceof PropertyImportedFromObject) {
                return ((PropertyImportedFromObject) descriptor).getCallableFromObject();
            }
            return descriptor;
        }
        return null;
    }


    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(
            @NotNull Instruction instruction,
            @NotNull BindingContext bindingContext
    ) {
        if (instruction instanceof VariableDeclarationInstruction) {
            KtDeclaration declaration = ((VariableDeclarationInstruction) instruction).getVariableDeclarationElement();
            return variableDescriptorForDeclaration(bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration));
        }
        else if (instruction instanceof AccessValueInstruction) {
            AccessTarget target = ((AccessValueInstruction) instruction).getTarget();
            if (target instanceof AccessTarget.Declaration) {
                return ((AccessTarget.Declaration) target).getDescriptor();
            }
            else if (target instanceof AccessTarget.Call) {
                return variableDescriptorForDeclaration(((AccessTarget.Call) target).getResolvedCall().getResultingDescriptor());
            }
        }
        return null;
    }

    // When deal with constructed object (not this) treat it like it's fully initialized
    // Otherwise (this or access with empty receiver) access instruction should be handled as usual
    public static boolean isThisOrNoDispatchReceiver(
            @NotNull AccessValueInstruction instruction,
            @NotNull BindingContext bindingContext
    ) {
        if (instruction.getReceiverValues().isEmpty()) {
            return true;
        }
        AccessTarget accessTarget = instruction.getTarget();
        if (accessTarget instanceof AccessTarget.BlackBox) return false;
        assert accessTarget instanceof AccessTarget.Call :
                "AccessTarget.Declaration has no receivers and it's not BlackBox, so it should be Call";

        ResolvedCall<?> accessResolvedCall = ((AccessTarget.Call) accessTarget).getResolvedCall();
        return ResolvedCallUtilKt.hasThisOrNoDispatchReceiver(accessResolvedCall, bindingContext);
    }
}
