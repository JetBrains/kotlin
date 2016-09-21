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

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsKt;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.utils.CollectionsKt;
import org.jetbrains.kotlin.utils.SmartSet;

import java.util.*;

public abstract class FunctionDescriptorImpl extends DeclarationDescriptorNonRootImpl implements FunctionDescriptor {
    private List<TypeParameterDescriptor> typeParameters;
    private List<ValueParameterDescriptor> unsubstitutedValueParameters;
    private KotlinType unsubstitutedReturnType;
    private ReceiverParameterDescriptor extensionReceiverParameter;
    private ReceiverParameterDescriptor dispatchReceiverParameter;
    private Modality modality;
    private Visibility visibility = Visibilities.UNKNOWN;
    private boolean isOperator = false;
    private boolean isInfix = false;
    private boolean isExternal = false;
    private boolean isInline = false;
    private boolean isTailrec = false;
    // Difference between these hidden kinds:
    // 1. isHiddenToOvercomeSignatureClash prohibit calling such functions even in super-call context
    // 2. isHiddenForResolutionEverywhereBesideSupercalls propagates to it's overrides descriptors while isHiddenToOvercomeSignatureClash does not
    private boolean isHiddenToOvercomeSignatureClash = false;
    private boolean isHiddenForResolutionEverywhereBesideSupercalls = false;
    private boolean isSuspend = false;
    private boolean hasStableParameterNames = true;
    private boolean hasSynthesizedParameterNames = false;
    private Collection<? extends FunctionDescriptor> overriddenFunctions = null;
    private volatile Function0<Set<FunctionDescriptor>> lazyOverriddenFunctionsTask = null;
    private final FunctionDescriptor original;
    private final Kind kind;
    @Nullable
    private FunctionDescriptor initialSignatureDescriptor = null;

    private Map<UserDataKey<?>, Object> userDataMap = null;

    protected FunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable FunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, annotations, name, source);
        this.original = original == null ? this : original;
        this.kind = kind;
    }

    @NotNull
    public FunctionDescriptorImpl initialize(
            @Nullable KotlinType receiverParameterType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable KotlinType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility
    ) {
        this.typeParameters = CollectionsKt.toReadOnlyList(typeParameters);
        this.unsubstitutedValueParameters = unsubstitutedValueParameters;
        this.unsubstitutedReturnType = unsubstitutedReturnType;
        this.modality = modality;
        this.visibility = visibility;
        this.extensionReceiverParameter = DescriptorFactory.createExtensionReceiverParameterForCallable(this, receiverParameterType);
        this.dispatchReceiverParameter = dispatchReceiverParameter;

        for (int i = 0; i < typeParameters.size(); ++i) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            if (typeParameterDescriptor.getIndex() != i) {
                throw new IllegalStateException(typeParameterDescriptor + " index is " + typeParameterDescriptor.getIndex() + " but position is " + i);
            }
        }

        for (int i = 0; i < unsubstitutedValueParameters.size(); ++i) {
            // TODO fill me
            int firstValueParameterOffset = 0; // receiverParameter.exists() ? 1 : 0;
            ValueParameterDescriptor valueParameterDescriptor = unsubstitutedValueParameters.get(i);
            if (valueParameterDescriptor.getIndex() != i + firstValueParameterOffset) {
                throw new IllegalStateException(valueParameterDescriptor + "index is " + valueParameterDescriptor.getIndex() + " but position is " + i);
            }
        }

        return this;
    }

    public void setVisibility(@NotNull Visibility visibility) {
        this.visibility = visibility;
    }

    public void setOperator(boolean isOperator) {
        this.isOperator = isOperator;
    }

    public void setInfix(boolean isInfix) {
        this.isInfix = isInfix;
    }

    public void setExternal(boolean isExternal) {
        this.isExternal = isExternal;
    }

    public void setInline(boolean isInline) {
        this.isInline = isInline;
    }

    public void setTailrec(boolean isTailrec) {
        this.isTailrec = isTailrec;
    }

    public void setHiddenToOvercomeSignatureClash(boolean hiddenToOvercomeSignatureClash) {
        isHiddenToOvercomeSignatureClash = hiddenToOvercomeSignatureClash;
    }

    private void setHiddenForResolutionEverywhereBesideSupercalls(boolean hiddenForResolutionEverywhereBesideSupercalls) {
        isHiddenForResolutionEverywhereBesideSupercalls = hiddenForResolutionEverywhereBesideSupercalls;
    }

    public void setSuspend(boolean suspend) {
        isSuspend = suspend;
    }

    public void setReturnType(@NotNull KotlinType unsubstitutedReturnType) {
        if (this.unsubstitutedReturnType != null) {
            // TODO: uncomment and fix tests
            //throw new IllegalStateException("returnType already set");
        }
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    public void setHasStableParameterNames(boolean hasStableParameterNames) {
        this.hasStableParameterNames = hasStableParameterNames;
    }

    public void setHasSynthesizedParameterNames(boolean hasSynthesizedParameterNames) {
        this.hasSynthesizedParameterNames = hasSynthesizedParameterNames;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getExtensionReceiverParameter() {
        return extensionReceiverParameter;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getDispatchReceiverParameter() {
        return dispatchReceiverParameter;
    }

    @NotNull
    @Override
    public Collection<? extends FunctionDescriptor> getOverriddenDescriptors() {
        performOverriddenLazyCalculationIfNeeded();
        return overriddenFunctions != null ? overriddenFunctions : Collections.<FunctionDescriptor>emptyList();
    }

    private void performOverriddenLazyCalculationIfNeeded() {
        Function0<Set<FunctionDescriptor>> overriddenTask = lazyOverriddenFunctionsTask;
        if (overriddenTask != null) {
            overriddenFunctions = overriddenTask.invoke();
            // Here it's important that this assignment is strictly after previous one
            // `lazyOverriddenFunctionsTask` is volatile, so when someone will see that it's null,
            // he can read consistent collection from `overriddenFunctions`,
            // because it's assignment happens-before of "lazyOverriddenFunctionsTask = null"
            lazyOverriddenFunctionsTask = null;
        }
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public boolean isOperator() {
        if (isOperator) return true;

        for (FunctionDescriptor descriptor : getOriginal().getOverriddenDescriptors()) {
            if (descriptor.isOperator()) return true;
        }

        return false;
    }

    @Override
    public boolean isInfix() {
        if (isInfix) return true;

        for (FunctionDescriptor descriptor : getOriginal().getOverriddenDescriptors()) {
            if (descriptor.isInfix()) return true;
        }

        return false;
    }

    @Override
    public boolean isExternal() {
        return isExternal;
    }

    @Override
    public boolean isInline() {
        return isInline;
    }

    @Override
    public boolean isTailrec() {
        return isTailrec;
    }

    @Override
    public boolean isSuspend() {
        return isSuspend;
    }

    @Override
    public <V> V getUserData(UserDataKey<V> key) {
        if (userDataMap == null) return null;
        //noinspection unchecked
        return (V) userDataMap.get(key);
    }

    @Override
    public boolean isHiddenToOvercomeSignatureClash() {
        return isHiddenToOvercomeSignatureClash;
    }

    @Override
    public void setOverriddenDescriptors(@NotNull Collection<? extends CallableMemberDescriptor> overriddenDescriptors) {
        //noinspection unchecked
        overriddenFunctions = (Collection<? extends FunctionDescriptor>) overriddenDescriptors;
        for (FunctionDescriptor function : overriddenFunctions) {
            if (function.isHiddenForResolutionEverywhereBesideSupercalls()) {
                isHiddenForResolutionEverywhereBesideSupercalls = true;
                break;
            }
        }
    }

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return unsubstitutedValueParameters;
    }

    @Override
    public boolean hasStableParameterNames() {
        return hasStableParameterNames;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        return hasSynthesizedParameterNames;
    }

    @Override
    public KotlinType getReturnType() {
        return unsubstitutedReturnType;
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @NotNull
    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public FunctionDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        return newCopyBuilder(originalSubstitutor).setOriginal(getOriginal()).build();
    }

    @Nullable
    protected KotlinType getExtensionReceiverParameterType() {
        if (extensionReceiverParameter == null) return null;
        return extensionReceiverParameter.getType();
    }

    @Override
    public boolean isHiddenForResolutionEverywhereBesideSupercalls() {
        return isHiddenForResolutionEverywhereBesideSupercalls;
    }

    public class CopyConfiguration implements SimpleFunctionDescriptor.CopyBuilder<FunctionDescriptor> {
        protected @NotNull TypeSubstitution substitution;
        protected @NotNull DeclarationDescriptor newOwner;
        protected @NotNull Modality newModality;
        protected @NotNull Visibility newVisibility;
        protected @Nullable FunctionDescriptor original = null;
        protected @NotNull Kind kind;
        protected @NotNull List<ValueParameterDescriptor> newValueParameterDescriptors;
        protected @Nullable KotlinType newExtensionReceiverParameterType;
        protected @Nullable ReceiverParameterDescriptor dispatchReceiverParameter = FunctionDescriptorImpl.this.dispatchReceiverParameter;
        protected @NotNull KotlinType newReturnType;
        protected @Nullable Name name;
        protected boolean copyOverrides = true;
        protected boolean signatureChange = false;
        protected boolean preserveSourceElement = false;
        protected boolean dropOriginalInContainingParts = false;
        private boolean isHiddenToOvercomeSignatureClash = isHiddenToOvercomeSignatureClash();
        private List<TypeParameterDescriptor> newTypeParameters = null;
        private Annotations additionalAnnotations = null;
        private boolean isHiddenForResolutionEverywhereBesideSupercalls = isHiddenForResolutionEverywhereBesideSupercalls();
        private SourceElement sourceElement;
        private Map<UserDataKey<?>, Object> userDataMap = new LinkedHashMap<UserDataKey<?>, Object>();
        private Boolean newHasSynthesizedParameterNames = null;

        public CopyConfiguration(
                @NotNull TypeSubstitution substitution,
                @NotNull DeclarationDescriptor newOwner,
                @NotNull Modality newModality,
                @NotNull Visibility newVisibility,
                @NotNull Kind kind,
                @NotNull List<ValueParameterDescriptor> newValueParameterDescriptors,
                @Nullable KotlinType newExtensionReceiverParameterType,
                @NotNull KotlinType newReturnType,
                @Nullable Name name
        ) {
            this.substitution = substitution;
            this.newOwner = newOwner;
            this.newModality = newModality;
            this.newVisibility = newVisibility;
            this.kind = kind;
            this.newValueParameterDescriptors = newValueParameterDescriptors;
            this.newExtensionReceiverParameterType = newExtensionReceiverParameterType;
            this.newReturnType = newReturnType;
            this.name = name;
        }

        @Override
        @NotNull
        public CopyConfiguration setOwner(@NotNull DeclarationDescriptor owner) {
            this.newOwner = owner;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setModality(@NotNull Modality modality) {
            this.newModality = modality;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setVisibility(@NotNull Visibility visibility) {
            this.newVisibility = visibility;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setKind(@NotNull Kind kind) {
            this.kind = kind;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setCopyOverrides(boolean copyOverrides) {
            this.copyOverrides = copyOverrides;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setName(@NotNull Name name) {
            this.name = name;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setValueParameters(@NotNull List<ValueParameterDescriptor> parameters) {
            this.newValueParameterDescriptors = parameters;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters) {
            this.newTypeParameters = parameters;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setReturnType(@NotNull KotlinType type) {
            this.newReturnType = type;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setExtensionReceiverType(@Nullable KotlinType type) {
            this.newExtensionReceiverParameterType = type;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setDispatchReceiverParameter(@Nullable ReceiverParameterDescriptor dispatchReceiverParameter) {
            this.dispatchReceiverParameter = dispatchReceiverParameter;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setOriginal(@Nullable FunctionDescriptor original) {
            this.original = original;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setSignatureChange() {
            this.signatureChange = true;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setPreserveSourceElement() {
            this.preserveSourceElement = true;
            return this;
        }

        @NotNull
        @Override
        public CopyBuilder<FunctionDescriptor> setSource(@NotNull SourceElement source) {
            this.sourceElement = source;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setDropOriginalInContainingParts() {
            this.dropOriginalInContainingParts = true;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setHiddenToOvercomeSignatureClash() {
            isHiddenToOvercomeSignatureClash = true;
            return this;
        }

        @Override
        @NotNull
        public CopyConfiguration setHiddenForResolutionEverywhereBesideSupercalls() {
            isHiddenForResolutionEverywhereBesideSupercalls = true;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setAdditionalAnnotations(@NotNull Annotations additionalAnnotations) {
            this.additionalAnnotations = additionalAnnotations;
            return this;
        }

        public CopyConfiguration setHasSynthesizedParameterNames(boolean value) {
            this.newHasSynthesizedParameterNames = value;
            return this;
        }

        @NotNull
        @Override
        public CopyConfiguration setSubstitution(@NotNull TypeSubstitution substitution) {
            this.substitution = substitution;
            return this;
        }

        @NotNull
        @Override
        public <V> CopyBuilder<FunctionDescriptor> putUserData(@NotNull UserDataKey<V> userDataKey, V value) {
            userDataMap.put(userDataKey, value);
            return this;
        }

        @Override
        @Nullable
        public FunctionDescriptor build() {
            return doSubstitute(this);
        }

        @Nullable
        public FunctionDescriptor getOriginal() {
            return original;
        }

        @NotNull
        public TypeSubstitution getSubstitution() {
            return substitution;
        }
    }

    @Override
    @NotNull
    public CopyBuilder<? extends FunctionDescriptor> newCopyBuilder() {
        return newCopyBuilder(TypeSubstitutor.EMPTY);
    }

    @NotNull
    protected CopyConfiguration newCopyBuilder(@NotNull TypeSubstitutor substitutor) {
        return new CopyConfiguration(
                substitutor.getSubstitution(),
                getContainingDeclaration(), getModality(), getVisibility(), getKind(), getValueParameters(),
                getExtensionReceiverParameterType(), getReturnType(), null);
    }

    @Nullable
    protected FunctionDescriptor doSubstitute(@NotNull CopyConfiguration configuration) {
        Annotations resultAnnotations =
                configuration.additionalAnnotations != null
                ? AnnotationsKt.composeAnnotations(getAnnotations(), configuration.additionalAnnotations)
                : getAnnotations();

        FunctionDescriptorImpl substitutedDescriptor = createSubstitutedCopy(
                configuration.newOwner, configuration.original, configuration.kind, configuration.name, resultAnnotations,
                getSourceToUseForCopy(configuration.preserveSourceElement, configuration.original, configuration.sourceElement));

        List<TypeParameterDescriptor> unsubstitutedTypeParameters =
                configuration.newTypeParameters == null ? getTypeParameters() : configuration.newTypeParameters;

        List<TypeParameterDescriptor> substitutedTypeParameters =
                new ArrayList<TypeParameterDescriptor>(unsubstitutedTypeParameters.size());
        final TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(
                unsubstitutedTypeParameters, configuration.substitution, substitutedDescriptor, substitutedTypeParameters
        );

        KotlinType substitutedReceiverParameterType = null;
        if (configuration.newExtensionReceiverParameterType != null) {
            substitutedReceiverParameterType = substitutor.substitute(configuration.newExtensionReceiverParameterType, Variance.IN_VARIANCE);
            if (substitutedReceiverParameterType == null) {
                return null;
            }
        }

        ReceiverParameterDescriptor substitutedExpectedThis = null;
        if (configuration.dispatchReceiverParameter != null) {
            // When generating fake-overridden member it's dispatch receiver parameter has type of Base, and it's correct.
            // E.g.
            // class Base { fun foo() }
            // class Derived : Base
            // val x: Base
            // if (x is Derived) {
            //    // `x` shouldn't be marked as smart-cast
            //    // but it would if fake-overridden `foo` had `Derived` as it's dispatch receiver parameter type
            //    x.foo()
            // }
            substitutedExpectedThis = configuration.dispatchReceiverParameter.substitute(substitutor);
            if (substitutedExpectedThis == null) {
                return null;
            }
        }

        List<ValueParameterDescriptor> substitutedValueParameters = getSubstitutedValueParameters(
                substitutedDescriptor, configuration.newValueParameterDescriptors, substitutor, configuration.dropOriginalInContainingParts
        );
        if (substitutedValueParameters == null) {
            return null;
        }

        KotlinType substitutedReturnType = substitutor.substitute(configuration.newReturnType, Variance.OUT_VARIANCE);
        if (substitutedReturnType == null) {
            return null;
        }

        substitutedDescriptor.initialize(
                substitutedReceiverParameterType,
                substitutedExpectedThis,
                substitutedTypeParameters,
                substitutedValueParameters,
                substitutedReturnType,
                configuration.newModality,
                configuration.newVisibility
        );
        substitutedDescriptor.setOperator(isOperator);
        substitutedDescriptor.setInfix(isInfix);
        substitutedDescriptor.setExternal(isExternal);
        substitutedDescriptor.setInline(isInline);
        substitutedDescriptor.setTailrec(isTailrec);
        substitutedDescriptor.setSuspend(isSuspend);
        substitutedDescriptor.setHasStableParameterNames(hasStableParameterNames);
        substitutedDescriptor.setHiddenToOvercomeSignatureClash(configuration.isHiddenToOvercomeSignatureClash);
        substitutedDescriptor.setHiddenForResolutionEverywhereBesideSupercalls(configuration.isHiddenForResolutionEverywhereBesideSupercalls);

        substitutedDescriptor.setHasSynthesizedParameterNames(
                configuration.newHasSynthesizedParameterNames != null ? configuration.newHasSynthesizedParameterNames : hasSynthesizedParameterNames
        );

        if (!configuration.userDataMap.isEmpty() || userDataMap != null) {
            Map<UserDataKey<?>, Object> newMap = configuration.userDataMap;

            if (userDataMap != null) {
                for (Map.Entry<UserDataKey<?>, Object> entry : userDataMap.entrySet()) {
                    if (!newMap.containsKey(entry.getKey())) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (newMap.size() == 1) {
                substitutedDescriptor.userDataMap =
                        Collections.<UserDataKey<?>, Object>singletonMap(
                                newMap.keySet().iterator().next(), newMap.values().iterator().next());
            }
            else {
                substitutedDescriptor.userDataMap = newMap;
            }
        }

        if (configuration.signatureChange || getInitialSignatureDescriptor() != null) {
            FunctionDescriptor initialSignature = (getInitialSignatureDescriptor() != null ? getInitialSignatureDescriptor() : this);
            FunctionDescriptor initialSignatureSubstituted = initialSignature.substitute(substitutor);
            substitutedDescriptor.setInitialSignatureDescriptor(initialSignatureSubstituted);
        }

        if (configuration.copyOverrides && !getOriginal().getOverriddenDescriptors().isEmpty()) {
            if (configuration.substitution.isEmpty()) {
                Function0<Set<FunctionDescriptor>> overriddenFunctionsTask = lazyOverriddenFunctionsTask;
                if (overriddenFunctionsTask != null) {
                    substitutedDescriptor.lazyOverriddenFunctionsTask = overriddenFunctionsTask;
                }
                else {
                    substitutedDescriptor.setOverriddenDescriptors(getOverriddenDescriptors());
                }
            }
            else {
                substitutedDescriptor.lazyOverriddenFunctionsTask = new Function0<Set<FunctionDescriptor>>() {
                    @Override
                    public Set<FunctionDescriptor> invoke() {
                        SmartSet<FunctionDescriptor> result = SmartSet.create();
                        for (FunctionDescriptor overriddenFunction : getOverriddenDescriptors()) {
                            result.add(overriddenFunction.substitute(substitutor));
                        }
                        return result;
                    }
                };
            }
        }

        return substitutedDescriptor;
    }

    @NotNull
    @Override
    public FunctionDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            Visibility visibility,
            Kind kind,
            boolean copyOverrides
    ) {
        return newCopyBuilder()
                .setOwner(newOwner)
                .setModality(modality)
                .setVisibility(visibility)
                .setKind(kind)
                .setCopyOverrides(copyOverrides)
                .build();
    }

    @NotNull
    protected abstract FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    );

    @NotNull
    private SourceElement getSourceToUseForCopy(
            boolean preserveSource,
            @Nullable FunctionDescriptor original,
            @Nullable SourceElement sourceElement
    ) {
        if (sourceElement != null) return sourceElement;
        return preserveSource
               ? (original != null ? original.getSource() : getOriginal().getSource())
               : SourceElement.NO_SOURCE;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(
            FunctionDescriptor substitutedDescriptor,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull TypeSubstitutor substitutor,
            boolean dropOriginal
    ) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(unsubstitutedValueParameters.size());
        for (ValueParameterDescriptor unsubstitutedValueParameter : unsubstitutedValueParameters) {
            // TODO : Lazy?
            KotlinType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType(), Variance.IN_VARIANCE);
            KotlinType varargElementType = unsubstitutedValueParameter.getVarargElementType();
            KotlinType substituteVarargElementType =
                    varargElementType == null ? null : substitutor.substitute(varargElementType, Variance.IN_VARIANCE);
            if (substitutedType == null) return null;
            result.add(
                    new ValueParameterDescriptorImpl(
                            substitutedDescriptor,
                            dropOriginal ? null : unsubstitutedValueParameter,
                            unsubstitutedValueParameter.getIndex(),
                            unsubstitutedValueParameter.getAnnotations(),
                            unsubstitutedValueParameter.getName(),
                            substitutedType,
                            unsubstitutedValueParameter.declaresDefaultValue(),
                            unsubstitutedValueParameter.isCrossinline(),
                            unsubstitutedValueParameter.isNoinline(),
                            unsubstitutedValueParameter.isCoroutine(),
                            substituteVarargElementType,
                            SourceElement.NO_SOURCE
                    )
            );
        }
        return result;
    }

    @Override
    @Nullable
    public FunctionDescriptor getInitialSignatureDescriptor() {
        return initialSignatureDescriptor;
    }

    private void setInitialSignatureDescriptor(@Nullable FunctionDescriptor initialSignatureDescriptor) {
        this.initialSignatureDescriptor = initialSignatureDescriptor;
    }
}
