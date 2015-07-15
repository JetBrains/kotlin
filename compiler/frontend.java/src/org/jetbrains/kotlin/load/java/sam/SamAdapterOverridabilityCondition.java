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

package org.jetbrains.kotlin.load.java.sam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.List;

public class SamAdapterOverridabilityCondition implements ExternalOverridabilityCondition {
    @Override
    public boolean isOverridable(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        if (!(subDescriptor instanceof SimpleFunctionDescriptor)) {
            return true;
        }

        SimpleFunctionDescriptor superOriginal = getOriginalOfSamAdapterFunction((SimpleFunctionDescriptor) superDescriptor);
        SimpleFunctionDescriptor subOriginal = getOriginalOfSamAdapterFunction((SimpleFunctionDescriptor) subDescriptor);
        if (superOriginal == null || subOriginal == null) { // super or sub is/overrides DECLARATION
            return subOriginal == null; // DECLARATION can override anything
        }

        // inheritor if SYNTHESIZED can override inheritor of SYNTHESIZED if their originals have same erasure
        return equalErasure(superOriginal, subOriginal);
    }

    private static boolean equalErasure(@NotNull FunctionDescriptor fun1, @NotNull FunctionDescriptor fun2) {
        List<ValueParameterDescriptor> parameters1 = fun1.getValueParameters();
        List<ValueParameterDescriptor> parameters2 = fun2.getValueParameters();

        for (ValueParameterDescriptor param1 : parameters1) {
            ValueParameterDescriptor param2 = parameters2.get(param1.getIndex());
            if (differentClasses(param2.getType(), param1.getType())) {
                return false;
            }
        }
        return true;
    }

    private static boolean differentClasses(@NotNull JetType type1, @NotNull JetType type2) {
        DeclarationDescriptor declarationDescriptor1 = type1.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor1 == null) return true; // No class, classes are not equal
        DeclarationDescriptor declarationDescriptor2 = type2.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor2 == null) return true; // Class of type1 is not null

        if (declarationDescriptor1 instanceof TypeParameterDescriptor && declarationDescriptor2 instanceof TypeParameterDescriptor) {
            // if type of value parameter is some generic parameter then their equality was checked by OverridingUtil before calling ExternalOverridabilityCondition
            // Note that it's true unless we generate sam adapter for type parameter with SAM interface as upper bound:
            // <K extends Runnable >void foo(K runnable) {}
            return false;
        }

        return !declarationDescriptor1.getOriginal().equals(declarationDescriptor2.getOriginal());
    }

    // if function is or overrides declaration, returns null; otherwise, return original of sam adapter with substituted type parameters
    @Nullable
    private static SimpleFunctionDescriptor getOriginalOfSamAdapterFunction(@NotNull SimpleFunctionDescriptor callable) {
        DeclarationDescriptor containingDeclaration = callable.getContainingDeclaration();
        if (!(containingDeclaration instanceof ClassDescriptor)) {
            return null;
        }
        SamAdapterInfo declarationOrSynthesized =
                getNearestDeclarationOrSynthesized(callable, ((ClassDescriptor) containingDeclaration).getDefaultType());

        if (declarationOrSynthesized == null) {
            return null;
        }

        SimpleFunctionDescriptorImpl fun = (SimpleFunctionDescriptorImpl) declarationOrSynthesized.samAdapter.getOriginal();
        if (!(fun instanceof SamAdapterFunctionDescriptor)) {
            return null;
        }

        SimpleFunctionDescriptor originalDeclarationOfSam = ((SamAdapterFunctionDescriptor) fun).getOriginForSam();

        return ((SimpleFunctionDescriptor) originalDeclarationOfSam.substitute(TypeSubstitutor.create(declarationOrSynthesized.ownerType)));
    }

    @Nullable
    private static SamAdapterInfo getNearestDeclarationOrSynthesized(
            @NotNull SimpleFunctionDescriptor samAdapter,
            @NotNull JetType ownerType
    ) {
        if (samAdapter.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return new SamAdapterInfo(samAdapter, ownerType);
        }

        for (CallableMemberDescriptor overridden : samAdapter.getOverriddenDescriptors()) {
            ClassDescriptor containingClass = (ClassDescriptor) overridden.getContainingDeclaration();

            for (JetType immediateSupertype : TypeUtils.getImmediateSupertypes(ownerType)) {
                if (containingClass != immediateSupertype.getConstructor().getDeclarationDescriptor()) {
                    continue;
                }

                SamAdapterInfo found = getNearestDeclarationOrSynthesized((SimpleFunctionDescriptor) overridden, immediateSupertype);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private static class SamAdapterInfo {
        private final SimpleFunctionDescriptor samAdapter;
        private final JetType ownerType;

        private SamAdapterInfo(@NotNull SimpleFunctionDescriptor samAdapter, @NotNull JetType ownerType) {
            this.samAdapter = samAdapter;
            this.ownerType = ownerType;
        }
    }
}
