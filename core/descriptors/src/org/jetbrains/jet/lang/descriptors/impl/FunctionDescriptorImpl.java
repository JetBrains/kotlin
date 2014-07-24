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

package org.jetbrains.jet.lang.descriptors.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DescriptorSubstitutor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class FunctionDescriptorImpl extends DeclarationDescriptorNonRootImpl implements FunctionDescriptor {

    protected List<TypeParameterDescriptor> typeParameters;
    protected List<ValueParameterDescriptor> unsubstitutedValueParameters;
    protected JetType unsubstitutedReturnType;
    private ReceiverParameterDescriptor receiverParameter;
    protected ReceiverParameterDescriptor expectedThisObject;
    protected Modality modality;

    protected Visibility visibility;
    protected final Set<FunctionDescriptor> overriddenFunctions = Sets.newLinkedHashSet(); // LinkedHashSet is essential here
    private final FunctionDescriptor original;
    private final Kind kind;

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
            @Nullable JetType receiverParameterType,
            @Nullable ReceiverParameterDescriptor expectedThisObject,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable JetType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility) {
        this.typeParameters = Lists.newArrayList(typeParameters);
        this.unsubstitutedValueParameters = unsubstitutedValueParameters;
        this.unsubstitutedReturnType = unsubstitutedReturnType;
        this.modality = modality;
        this.visibility = visibility;
        this.receiverParameter = DescriptorFactory.createReceiverParameterForCallable(this, receiverParameterType);
        this.expectedThisObject = expectedThisObject;
        
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

    public void setReturnType(@NotNull JetType unsubstitutedReturnType) {
        if (this.unsubstitutedReturnType != null) {
            // TODO: uncomment and fix tests
            //throw new IllegalStateException("returnType already set");
        }
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getReceiverParameter() {
        return receiverParameter;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getExpectedThisObject() {
        return expectedThisObject;
    }

    @NotNull
    @Override
    public Set<? extends FunctionDescriptor> getOverriddenDescriptors() {
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
        return true;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        return false;
    }

    @Override
    public JetType getReturnType() {
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
    public final FunctionDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        return doSubstitute(originalSubstitutor, getContainingDeclaration(), modality, visibility, getOriginal(), true, getKind());
    }

    @Nullable
    protected FunctionDescriptor doSubstitute(@NotNull TypeSubstitutor originalSubstitutor,
            @NotNull DeclarationDescriptor newOwner,
            @NotNull Modality newModality,
            @NotNull Visibility newVisibility,
            @Nullable FunctionDescriptor original,
            boolean copyOverrides,
            @NotNull Kind kind
    ) {
        FunctionDescriptorImpl substitutedDescriptor = createSubstitutedCopy(newOwner, original, kind);

        List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

        JetType substitutedReceiverParameterType = null;
        if (receiverParameter != null) {
            substitutedReceiverParameterType = substitutor.substitute(getReceiverParameter().getType(), Variance.IN_VARIANCE);
            if (substitutedReceiverParameterType == null) {
                return null;
            }
        }

        ReceiverParameterDescriptor substitutedExpectedThis = null;
        if (expectedThisObject != null) {
            substitutedExpectedThis = expectedThisObject.substitute(substitutor);
            if (substitutedExpectedThis == null) {
                return null;
            }
        }

        List<ValueParameterDescriptor> substitutedValueParameters = getSubstitutedValueParameters(substitutedDescriptor, this, substitutor);
        if (substitutedValueParameters == null) {
            return null;
        }

        JetType substitutedReturnType = substitutor.substitute(getReturnType(), Variance.OUT_VARIANCE);
        if (substitutedReturnType == null) {
            return null;
        }

        substitutedDescriptor.initialize(
                substitutedReceiverParameterType,
                substitutedExpectedThis,
                substitutedTypeParameters,
                substitutedValueParameters,
                substitutedReturnType,
                newModality,
                newVisibility
        );
        if (copyOverrides) {
            for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
                OverridingUtil.bindOverride(substitutedDescriptor, overriddenFunction.substitute(substitutor));
            }
        }
        return substitutedDescriptor;
    }

    @NotNull
    protected abstract FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind
    );

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(FunctionDescriptor substitutedDescriptor, @NotNull FunctionDescriptor functionDescriptor, @NotNull TypeSubstitutor substitutor) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getValueParameters();
        for (ValueParameterDescriptor unsubstitutedValueParameter : unsubstitutedValueParameters) {
            // TODO : Lazy?
            JetType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getType(), Variance.IN_VARIANCE);
            JetType varargElementType = unsubstitutedValueParameter.getVarargElementType();
            JetType substituteVarargElementType = varargElementType == null ? null : substitutor.substitute(varargElementType, Variance.IN_VARIANCE);
            if (substitutedType == null) return null;
            result.add(new ValueParameterDescriptorImpl(
                    substitutedDescriptor,
                    unsubstitutedValueParameter,
                    unsubstitutedValueParameter.getIndex(),
                    unsubstitutedValueParameter.getAnnotations(),
                    unsubstitutedValueParameter.getName(),
                    substitutedType,
                    unsubstitutedValueParameter.declaresDefaultValue(),
                    substituteVarargElementType,
                    SourceElement.NO_SOURCE
                    )
            );
        }
        return result;
    }
}
