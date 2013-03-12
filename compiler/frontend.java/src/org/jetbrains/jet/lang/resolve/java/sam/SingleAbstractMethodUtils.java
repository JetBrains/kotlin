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

package org.jetbrains.jet.lang.resolve.java.sam;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SingleAbstractMethodUtils {
    public static boolean isFunctionalInterface(@NotNull ClassDescriptor klass) {
        if (klass.getKind() != ClassKind.TRAIT) {
            return false;
        }

        List<CallableMemberDescriptor> abstractMembers = getAbstractMembers(klass);
        return abstractMembers.size() == 1 && abstractMembers.get(0) instanceof SimpleFunctionDescriptor;
    }

    @NotNull
    private static List<CallableMemberDescriptor> getAbstractMembers(@NotNull ClassDescriptor klass) {
        List<CallableMemberDescriptor> abstractMembers = Lists.newArrayList();
        for (DeclarationDescriptor member : klass.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (member instanceof CallableMemberDescriptor && ((CallableMemberDescriptor) member).getModality() == Modality.ABSTRACT) {
                abstractMembers.add((CallableMemberDescriptor) member);
            }
        }
        return abstractMembers;
    }

    @NotNull
    private static JetType getFunctionalTypeForFunction(@NotNull FunctionDescriptor function) {
        // TODO substitute type parameters of class
        // TODO substitute type parameters of function with star projections
        JetType returnType = function.getReturnType();
        assert returnType != null : "function is not initialized: " + function;
        List<JetType> parameterTypes = Lists.newArrayList();
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            parameterTypes.add(parameter.getType());
        }
        return KotlinBuiltIns.getInstance().getFunctionType(Collections.<AnnotationDescriptor>emptyList(), null, parameterTypes, returnType);
    }

    public static SimpleFunctionDescriptor createConstructorFunction(@NotNull ClassDescriptor klass) {
        SimpleFunctionDescriptorImpl result = new SimpleFunctionDescriptorImpl(
                klass.getContainingDeclaration(),
                klass.getAnnotations(),
                klass.getName(),
                CallableMemberDescriptor.Kind.SYNTHESIZED
        );

        JetType parameterType = getFunctionalTypeForFunction((SimpleFunctionDescriptor) getAbstractMembers(klass).get(0));
        ValueParameterDescriptor parameter = new ValueParameterDescriptorImpl(
                result, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("function"), parameterType, false, null);

        result.initialize(
                null,
                null,
                Lists.<TypeParameterDescriptor>newArrayList(), // TODO recreate type parameters of class
                Arrays.asList(parameter),
                klass.getDefaultType(), // TODO substitute type parameters of class
                Modality.FINAL,
                klass.getVisibility(),
                false
        );

        return result;
    }

    private SingleAbstractMethodUtils() {
    }
}
