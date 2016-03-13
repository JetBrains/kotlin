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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;

public class JavaConstructorDescriptor extends ConstructorDescriptorImpl implements JavaCallableMemberDescriptor {
    private Boolean hasStableParameterNames = null;
    private Boolean hasSynthesizedParameterNames = null;

    protected JavaConstructorDescriptor(
            @NotNull ClassDescriptor containingDeclaration,
            @Nullable JavaConstructorDescriptor original,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, original, annotations, isPrimary, kind, source);
    }

    @NotNull
    public static JavaConstructorDescriptor createJavaConstructor(
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull SourceElement source
    ) {
        return new JavaConstructorDescriptor(containingDeclaration, null, annotations, isPrimary, Kind.DECLARATION, source);
    }

    @Override
    public boolean hasStableParameterNames() {
        assert hasStableParameterNames != null : "hasStableParameterNames was not set: " + this;
        return hasStableParameterNames;
    }

    public void setHasStableParameterNames(boolean hasStableParameterNames) {
        this.hasStableParameterNames = hasStableParameterNames;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        assert hasSynthesizedParameterNames != null : "hasSynthesizedParameterNames was not set: " + this;
        return hasSynthesizedParameterNames;
    }

    public void setHasSynthesizedParameterNames(boolean hasSynthesizedParameterNames) {
        this.hasSynthesizedParameterNames = hasSynthesizedParameterNames;
    }

    @NotNull
    @Override
    protected JavaConstructorDescriptor createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            boolean preserveSource
    ) {
        if (kind != Kind.DECLARATION && kind != Kind.SYNTHESIZED) {
            throw new IllegalStateException(
                    "Attempt at creating a constructor that is not a declaration: \n" +
                    "copy from: " + this + "\n" +
                    "newOwner: " + newOwner + "\n" +
                    "kind: " + kind
            );
        }

        assert newName == null : "Attempt to rename constructor: " + this;

        JavaConstructorDescriptor result = createDescriptor((ClassDescriptor) newOwner, (JavaConstructorDescriptor) original, kind,
                                                            getSourceToUseForCopy(preserveSource, original));
        result.setHasStableParameterNames(hasStableParameterNames());
        result.setHasSynthesizedParameterNames(hasSynthesizedParameterNames());
        return result;
    }

    @NotNull
    protected JavaConstructorDescriptor createDescriptor(
            @NotNull ClassDescriptor newOwner,
            @Nullable JavaConstructorDescriptor original,
            @NotNull Kind kind,
            @NotNull SourceElement sourceElement
    ) {
        return new JavaConstructorDescriptor(
                newOwner, original, getAnnotations(), isPrimary, kind,
                sourceElement
        );
    }

    @Override
    @NotNull
    public JavaConstructorDescriptor enhance(
            @Nullable KotlinType enhancedReceiverType,
            @NotNull List<KotlinType> enhancedValueParametersTypes,
            @NotNull KotlinType enhancedReturnType
    ) {
        JavaConstructorDescriptor enhanced = createSubstitutedCopy(getContainingDeclaration(), /* original = */ null, getKind(), null, true);
        // We do not use doSubstitute here as in JavaMethodDescriptor.enhance because type parameters of constructor belongs to class
        enhanced.initialize(
                enhancedReceiverType,
                getDispatchReceiverParameter(),
                getTypeParameters(),
                UtilKt.copyValueParameters(enhancedValueParametersTypes, getValueParameters(), enhanced),
                enhancedReturnType,
                getModality(),
                getVisibility()
        );

        return enhanced;
    }

}
