package org.jetbrains.jet.lang.resolve.java.sam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.ExternalOverridabilityCondition;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.List;

public class SamAdapterOverridabilityCondition implements ExternalOverridabilityCondition {
    @Override
    public boolean isOverridable(@NotNull CallableDescriptor superDescriptor, @NotNull CallableDescriptor subDescriptor) {
        if (subDescriptor instanceof PropertyDescriptor) {
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
            if (!TypeUtils.equalClasses(param2.getType(), param1.getType())) {
                return false;
            }
        }
        return true;
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

        SimpleFunctionDescriptor originalDeclarationOfSam = ((SamAdapterFunctionDescriptor) fun).getBaseForSynthesized();

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
