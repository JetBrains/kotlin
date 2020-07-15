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

package org.jetbrains.kotlin.load.java.descriptors;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.OperatorChecks;

import java.util.List;
import java.util.Map;

public class JavaMethodDescriptor extends SimpleFunctionDescriptorImpl implements JavaCallableMemberDescriptor {
    // TODO: It's only used to retrieve annotations from the original value parameter and can be removed when
    // org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl.initialize accepts extension parameter descriptor
    // instead of type
    public static final UserDataKey<ValueParameterDescriptor> ORIGINAL_VALUE_PARAMETER_FOR_EXTENSION_RECEIVER =
            new UserDataKey<ValueParameterDescriptor>() {};

    public static final UserDataKey<Boolean> HAS_ERASED_VALUE_PARAMETERS = new UserDataKey<Boolean>() {};

    private enum ParameterNamesStatus {
        NON_STABLE_DECLARED(false, false),
        STABLE_DECLARED(true, false),
        NON_STABLE_SYNTHESIZED(false, true),
        STABLE_SYNTHESIZED(true, true), // TODO: this makes no sense
        ;

        public final boolean isStable;
        public final boolean isSynthesized;

        ParameterNamesStatus(boolean isStable, boolean isSynthesized) {
            this.isStable = isStable;
            this.isSynthesized = isSynthesized;
        }

        @NotNull
        public static ParameterNamesStatus get(boolean stable, boolean synthesized) {
            return stable ? (synthesized ? STABLE_SYNTHESIZED : STABLE_DECLARED) :
                   (synthesized ? NON_STABLE_SYNTHESIZED : NON_STABLE_DECLARED);
        }
    }

    private ParameterNamesStatus parameterNamesStatus = null;
    private final boolean isForRecordComponent;

    protected JavaMethodDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source,
            boolean isForRecordComponent
    ) {
        super(containingDeclaration, original, annotations, name, kind, source);
        this.isForRecordComponent = isForRecordComponent;
    }

    @NotNull
    public static JavaMethodDescriptor createJavaMethod(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source,
            boolean isForRecordComponent
    ) {
        return new JavaMethodDescriptor(containingDeclaration, null, annotations, name, Kind.DECLARATION, source, isForRecordComponent);
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<ReceiverParameterDescriptor> contextReceiverParameters,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable KotlinType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull DescriptorVisibility visibility,
            @Nullable Map<? extends UserDataKey<?>, ?> userData
    ) {
        SimpleFunctionDescriptorImpl descriptor = super.initialize(
                extensionReceiverParameter, dispatchReceiverParameter, contextReceiverParameters, typeParameters, unsubstitutedValueParameters,
                unsubstitutedReturnType, modality, visibility, userData
        );
        setOperator(OperatorChecks.INSTANCE.check(descriptor).isSuccess());
        return descriptor;
    }

    @Override
    public boolean hasStableParameterNames() {
        assert parameterNamesStatus != null : "Parameter names status was not set: " + this;
        return parameterNamesStatus.isStable;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        assert parameterNamesStatus != null : "Parameter names status was not set: " + this;
        return parameterNamesStatus.isSynthesized;
    }

    public void setParameterNamesStatus(boolean hasStableParameterNames, boolean hasSynthesizedParameterNames) {
        this.parameterNamesStatus = ParameterNamesStatus.get(hasStableParameterNames, hasSynthesizedParameterNames);
    }

    public boolean isForRecordComponent() {
        return isForRecordComponent;
    }

    @NotNull
    @Override
    protected JavaMethodDescriptor createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        JavaMethodDescriptor result = new JavaMethodDescriptor(
                newOwner,
                (SimpleFunctionDescriptor) original,
                annotations,
                newName != null ? newName : getName(),
                kind,
                source,
                isForRecordComponent
        );
        result.setParameterNamesStatus(hasStableParameterNames(), hasSynthesizedParameterNames());
        return result;
    }

    @Override
    @NotNull
    public JavaMethodDescriptor enhance(
            @Nullable KotlinType enhancedReceiverType,
            @NotNull List<KotlinType> enhancedValueParameterTypes,
            @NotNull KotlinType enhancedReturnType,
            @Nullable Pair<UserDataKey<?>, ?> additionalUserData
    ) {
        List<ValueParameterDescriptor> enhancedValueParameters =
                UtilKt.copyValueParameters(enhancedValueParameterTypes, getValueParameters(), this);

        ReceiverParameterDescriptor enhancedReceiver =
                enhancedReceiverType == null ? null : DescriptorFactory.createExtensionReceiverParameterForCallable(
                        this, enhancedReceiverType, Annotations.Companion.getEMPTY()
                );

        JavaMethodDescriptor enhancedMethod =
                (JavaMethodDescriptor) newCopyBuilder()
                        .setValueParameters(enhancedValueParameters)
                        .setReturnType(enhancedReturnType)
                        .setExtensionReceiverParameter(enhancedReceiver)
                        .setDropOriginalInContainingParts()
                        .setPreserveSourceElement()
                        .build();

        assert enhancedMethod != null : "null after substitution while enhancing " + this;

        if (additionalUserData != null) {
            enhancedMethod.putInUserDataMap(additionalUserData.getFirst(), additionalUserData.getSecond());
        }

        return enhancedMethod;
    }
}
