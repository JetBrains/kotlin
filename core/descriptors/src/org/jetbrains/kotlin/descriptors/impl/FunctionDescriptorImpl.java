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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.types.DescriptorSubstitutor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.kotlin.utils.CollectionsKt;
import org.jetbrains.kotlin.utils.SmartSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    private boolean hasStableParameterNames = true;
    private boolean hasSynthesizedParameterNames = false;
    private final Set<FunctionDescriptor> overriddenFunctions = SmartSet.create();
    private final FunctionDescriptor original;
    private final Kind kind;
    @Nullable
    private FunctionDescriptor initialSignatureDescriptor = null;

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
        return overriddenFunctions;
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

        for (FunctionDescriptor descriptor : overriddenFunctions) {
            if (descriptor.isOperator()) return true;
        }

        return false;
    }

    @Override
    public boolean isInfix() {
        if (isInfix) return true;

        for (FunctionDescriptor descriptor : overriddenFunctions) {
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
    public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overriddenFunction) {
        overriddenFunctions.add((FunctionDescriptor) overriddenFunction);
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
        return doSubstitute(newCopyBuilder(originalSubstitutor).setOriginal(getOriginal()));
    }

    @Nullable
    protected KotlinType getExtensionReceiverParameterType() {
        if (extensionReceiverParameter == null) return null;
        return extensionReceiverParameter.getType();
    }

    public class CopyConfiguration {
        protected @NotNull TypeSubstitutor originalSubstitutor;
        protected @NotNull DeclarationDescriptor newOwner;
        protected @NotNull Modality newModality;
        protected @NotNull Visibility newVisibility;
        protected @Nullable FunctionDescriptor original = null;
        protected @NotNull Kind kind;
        protected @NotNull List<ValueParameterDescriptor> newValueParameterDescriptors;
        protected @Nullable KotlinType newExtensionReceiverParameterType;
        protected @NotNull KotlinType newReturnType;
        protected @Nullable Name name;
        protected boolean copyOverrides;
        protected boolean signatureChange;
        private List<TypeParameterDescriptor> newTypeParameters = null;

        public CopyConfiguration(
                @NotNull TypeSubstitutor originalSubstitutor,
                @NotNull DeclarationDescriptor newOwner,
                @NotNull Modality newModality,
                @NotNull Visibility newVisibility,
                @NotNull Kind kind,
                @NotNull List<ValueParameterDescriptor> newValueParameterDescriptors,
                @Nullable KotlinType newExtensionReceiverParameterType,
                @NotNull KotlinType newReturnType,
                @Nullable Name name
        ) {
            this.originalSubstitutor = originalSubstitutor;
            this.newOwner = newOwner;
            this.newModality = newModality;
            this.newVisibility = newVisibility;
            this.kind = kind;
            this.newValueParameterDescriptors = newValueParameterDescriptors;
            this.newExtensionReceiverParameterType = newExtensionReceiverParameterType;
            this.newReturnType = newReturnType;
            this.name = name;
            this.copyOverrides = true;
            this.signatureChange = false;
        }

        @NotNull
        protected CopyConfiguration setOwner(@NotNull DeclarationDescriptor owner) {
            this.newOwner = owner;
            return this;
        }

        @NotNull
        public CopyConfiguration setModality(@NotNull Modality modality) {
            this.newModality = modality;
            return this;
        }

        @NotNull
        public CopyConfiguration setVisibility(@NotNull Visibility visibility) {
            this.newVisibility = visibility;
            return this;
        }

        @NotNull
        public CopyConfiguration setKind(@NotNull Kind kind) {
            this.kind = kind;
            return this;
        }

        @NotNull
        public CopyConfiguration setCopyOverrides(boolean copyOverrides) {
            this.copyOverrides = copyOverrides;
            return this;
        }

        @NotNull
        public CopyConfiguration setName(@NotNull Name name) {
            this.name = name;
            return this;
        }

        @NotNull
        public CopyConfiguration setValueParameters(@NotNull List<ValueParameterDescriptor> parameters) {
            this.newValueParameterDescriptors = parameters;
            return this;
        }

        @NotNull
        public CopyConfiguration setTypeParameters(@NotNull List<TypeParameterDescriptor> parameters) {
            this.newTypeParameters = parameters;
            return this;
        }

        public CopyConfiguration setReturnType(@NotNull KotlinType type) {
            this.newReturnType = type;
            return this;
        }

        public CopyConfiguration setExtensionReceiverType(@Nullable KotlinType type) {
            this.newExtensionReceiverParameterType = type;
            return this;
        }

        @NotNull
        public CopyConfiguration setOriginal(@NotNull FunctionDescriptor original) {
            this.original = original;
            return this;
        }

        @NotNull
        public CopyConfiguration setSignatureChange() {
            this.signatureChange = true;
            return this;
        }

        public FunctionDescriptor build() {
            return doSubstitute(this);
        }

        @Nullable
        public FunctionDescriptor getOriginal() {
            return original;
        }

        @NotNull
        public TypeSubstitutor getOriginalSubstitutor() {
            return originalSubstitutor;
        }
    }

    @NotNull
    public CopyConfiguration newCopyBuilder() {
        return newCopyBuilder(TypeSubstitutor.EMPTY);
    }

    @NotNull
    private CopyConfiguration newCopyBuilder(@NotNull TypeSubstitutor substitutor) {
        return new CopyConfiguration(
                substitutor,
                getContainingDeclaration(), getModality(), getVisibility(), getKind(), getValueParameters(),
                getExtensionReceiverParameterType(), getReturnType(), null);
    }

    @Nullable
    protected FunctionDescriptor doSubstitute(@NotNull CopyConfiguration configuration) {
        FunctionDescriptorImpl substitutedDescriptor = createSubstitutedCopy(
                configuration.newOwner, configuration.original, configuration.kind, configuration.name,
                /* preserveSource = */ configuration.signatureChange);

        List<TypeParameterDescriptor> substitutedTypeParameters;
        TypeSubstitutor substitutor;

        if (configuration.newTypeParameters == null) {
            List<TypeParameterDescriptor> originalTypeParameters = getTypeParameters();
            substitutedTypeParameters = new ArrayList<TypeParameterDescriptor>(originalTypeParameters.size());
            substitutor = DescriptorSubstitutor.substituteTypeParameters(
                    originalTypeParameters, configuration.originalSubstitutor.getSubstitution(), substitutedDescriptor, substitutedTypeParameters
            );
        }
        else {
            // They should be already substituted
            substitutedTypeParameters = configuration.newTypeParameters;
            substitutor = configuration.originalSubstitutor;
        }

        KotlinType substitutedReceiverParameterType = null;
        if (configuration.newExtensionReceiverParameterType != null) {
            substitutedReceiverParameterType = substitutor.substitute(configuration.newExtensionReceiverParameterType, Variance.IN_VARIANCE);
            if (substitutedReceiverParameterType == null) {
                return null;
            }
        }

        ReceiverParameterDescriptor substitutedExpectedThis = null;
        if (dispatchReceiverParameter != null) {
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
            substitutedExpectedThis = dispatchReceiverParameter.substitute(substitutor);
            if (substitutedExpectedThis == null) {
                return null;
            }
        }

        List<ValueParameterDescriptor> substitutedValueParameters = getSubstitutedValueParameters(
                substitutedDescriptor, configuration.newValueParameterDescriptors, substitutor
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
        substitutedDescriptor.setHasStableParameterNames(hasStableParameterNames);
        substitutedDescriptor.setHasSynthesizedParameterNames(hasSynthesizedParameterNames);

        if (configuration.signatureChange || getInitialSignatureDescriptor() != null) {
            FunctionDescriptor initialSignature = (getInitialSignatureDescriptor() != null ? getInitialSignatureDescriptor() : this);
            FunctionDescriptor initialSignatureSubstituted = initialSignature.substitute(substitutor);
            substitutedDescriptor.setInitialSignatureDescriptor(initialSignatureSubstituted);
        }

        if (configuration.copyOverrides) {
            for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
                substitutedDescriptor.addOverriddenDescriptor(overriddenFunction.substitute(substitutor));
            }
        }

        return substitutedDescriptor;
    }

    @NotNull
    protected abstract FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            boolean preserveSource
    );

    @NotNull
    protected SourceElement getSourceToUseForCopy(boolean preserveSource, @Nullable FunctionDescriptor original) {
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
            @NotNull TypeSubstitutor substitutor
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
                            unsubstitutedValueParameter,
                            unsubstitutedValueParameter.getIndex(),
                            unsubstitutedValueParameter.getAnnotations(),
                            unsubstitutedValueParameter.getName(),
                            substitutedType,
                            unsubstitutedValueParameter.declaresDefaultValue(),
                            unsubstitutedValueParameter.isCrossinline(),
                            unsubstitutedValueParameter.isNoinline(),
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

    public void setInitialSignatureDescriptor(@Nullable FunctionDescriptor initialSignatureDescriptor) {
        this.initialSignatureDescriptor = initialSignatureDescriptor;
    }
}
