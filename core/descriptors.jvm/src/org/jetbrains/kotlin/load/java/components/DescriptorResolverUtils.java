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

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DescriptorResolverUtils {
    private DescriptorResolverUtils() {
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Collection<D> resolveOverridesForNonStaticMembers(
        @NotNull Name name, @NotNull Collection<D> membersFromSupertypes, @NotNull Collection<D> membersFromCurrent,
        @NotNull ClassDescriptor classDescriptor, @NotNull ErrorReporter errorReporter
) {
        return resolveOverrides(name, membersFromSupertypes, membersFromCurrent, classDescriptor, errorReporter, false);
    }

    @NotNull
    public static <D extends CallableMemberDescriptor> Collection<D> resolveOverridesForStaticMembers(
        @NotNull Name name, @NotNull Collection<D> membersFromSupertypes, @NotNull Collection<D> membersFromCurrent,
        @NotNull ClassDescriptor classDescriptor, @NotNull ErrorReporter errorReporter
) {
        return resolveOverrides(name, membersFromSupertypes, membersFromCurrent, classDescriptor, errorReporter, true);
    }

    @NotNull
    private static <D extends CallableMemberDescriptor> Collection<D> resolveOverrides(
            @NotNull Name name,
            @NotNull Collection<D> membersFromSupertypes,
            @NotNull Collection<D> membersFromCurrent,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull final ErrorReporter errorReporter,
            final boolean isStaticContext
    ) {
        final Set<D> result = new LinkedHashSet<D>();

        OverridingUtil.generateOverridesInFunctionGroup(
                name, membersFromSupertypes, membersFromCurrent, classDescriptor,
                new NonReportingOverrideStrategy() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void addFakeOverride(@NotNull CallableMemberDescriptor fakeOverride) {
                        OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, new Function1<CallableMemberDescriptor, Unit>() {
                            @Override
                            public Unit invoke(@NotNull CallableMemberDescriptor descriptor) {
                                errorReporter.reportCannotInferVisibility(descriptor);
                                return Unit.INSTANCE;
                            }
                        });
                        result.add((D) fakeOverride);
                    }

                    @Override
                    public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                        // nop
                    }

                    @Override
                    public void setOverriddenDescriptors(
                            @NotNull CallableMemberDescriptor member, @NotNull Collection<? extends CallableMemberDescriptor> overridden
                    ) {
                        // do not set overridden descriptors for declared static fields and methods from java
                        if (isStaticContext && member.getKind() != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                            return;
                        }
                        super.setOverriddenDescriptors(member, overridden);
                    }
                }
        );

        return result;
    }

    @Nullable
    public static ValueParameterDescriptor getAnnotationParameterByName(@NotNull Name name, @NotNull ClassDescriptor annotationClass) {
        Collection<ClassConstructorDescriptor> constructors = annotationClass.getConstructors();
        if (constructors.size() != 1) return null;

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
