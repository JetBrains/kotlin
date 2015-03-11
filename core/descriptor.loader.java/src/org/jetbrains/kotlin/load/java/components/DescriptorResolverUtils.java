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

package org.jetbrains.kotlin.load.java.components;

import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DescriptorResolverUtils {
    private DescriptorResolverUtils() {
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Collection<D> resolveOverrides(
            @NotNull Name name,
            @NotNull Collection<D> membersFromSupertypes,
            @NotNull Collection<D> membersFromCurrent,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull final ErrorReporter errorReporter
    ) {
        final Set<D> result = new HashSet<D>();

        OverridingUtil.generateOverridesInFunctionGroup(
                name, membersFromSupertypes, membersFromCurrent, classDescriptor,
                new OverridingUtil.DescriptorSink() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, new Function1<CallableMemberDescriptor, Unit>() {
                            @Override
                            public Unit invoke(@NotNull CallableMemberDescriptor descriptor) {
                                errorReporter.reportCannotInferVisibility(descriptor);
                                return Unit.INSTANCE$;
                            }
                        });
                        result.add((D) fakeOverride);
                    }

                    @Override
                    public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                        // nop
                    }
                }
        );

        return result;
    }

    @Nullable
    public static ValueParameterDescriptor getAnnotationParameterByName(@NotNull Name name, @NotNull ClassDescriptor annotationClass) {
        Collection<ConstructorDescriptor> constructors = annotationClass.getConstructors();
        assert constructors.size() == 1 : "Annotation class descriptor must have only one constructor";

        for (ValueParameterDescriptor parameter : constructors.iterator().next().getValueParameters()) {
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }

        return null;
    }

    public static boolean isObjectMethodInInterface(@NotNull JavaMember member) {
        return member.getContainingClass().isInterface() && member instanceof JavaMethod && isObjectMethod((JavaMethod) member);
    }

    public static boolean isObjectMethod(@NotNull JavaMethod method) {
        String name = method.getName().asString();
        if (name.equals("toString") || name.equals("hashCode")) {
            return method.getValueParameters().isEmpty();
        }
        else if (name.equals("equals")) {
            return isMethodWithOneParameterWithFqName(method, "java.lang.Object");
        }
        return false;
    }

    private static boolean isMethodWithOneParameterWithFqName(@NotNull JavaMethod method, @NotNull String fqName) {
        List<JavaValueParameter> parameters = method.getValueParameters();
        if (parameters.size() == 1) {
            JavaType type = parameters.get(0).getType();
            if (type instanceof JavaClassifierType) {
                JavaClassifier classifier = ((JavaClassifierType) type).getClassifier();
                if (classifier instanceof JavaClass) {
                    FqName classFqName = ((JavaClass) classifier).getFqName();
                    return classFqName != null && classFqName.asString().equals(fqName);
                }
            }
        }
        return false;
    }
}
