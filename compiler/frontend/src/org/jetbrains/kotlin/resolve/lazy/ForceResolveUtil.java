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

package org.jetbrains.kotlin.resolve.lazy;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypesPackage;

import java.util.Collection;

public class ForceResolveUtil {
    private static final Logger LOG = Logger.getInstance(ForceResolveUtil.class);

    private ForceResolveUtil() {}

    public static <T extends DeclarationDescriptor> T forceResolveAllContents(@NotNull T descriptor) {
        doForceResolveAllContents(descriptor);
        return descriptor;
    }

    public static void forceResolveAllContents(@NotNull JetScope scope) {
        forceResolveAllContents(scope.getAllDescriptors());
    }

    public static void forceResolveAllContents(@NotNull Iterable<? extends DeclarationDescriptor> descriptors) {
        for (DeclarationDescriptor descriptor : descriptors) {
            forceResolveAllContents(descriptor);
        }
    }

    public static void forceResolveAllContents(@NotNull Collection<JetType> types) {
        for (JetType type : types) {
            forceResolveAllContents(type);
        }
    }

    public static void forceResolveAllContents(@NotNull TypeConstructor typeConstructor) {
        doForceResolveAllContents(typeConstructor);
    }

    public static void forceResolveAllContents(@NotNull Annotations annotations) {
        doForceResolveAllContents(annotations);
        for (AnnotationDescriptor annotation : annotations) {
            doForceResolveAllContents(annotation);
        }
    }

    private static void doForceResolveAllContents(Object object) {
        if (object instanceof LazyEntity) {
            LazyEntity lazyEntity = (LazyEntity) object;
            lazyEntity.forceResolveAllContents();
        }
        else if (object instanceof CallableDescriptor) {
            CallableDescriptor callableDescriptor = (CallableDescriptor) object;
            ReceiverParameterDescriptor parameter = callableDescriptor.getExtensionReceiverParameter();
            if (parameter != null) {
                forceResolveAllContents(parameter.getType());
            }
            for (ValueParameterDescriptor parameterDescriptor : callableDescriptor.getValueParameters()) {
                forceResolveAllContents(parameterDescriptor);
            }
            for (TypeParameterDescriptor typeParameterDescriptor : callableDescriptor.getTypeParameters()) {
                forceResolveAllContents(typeParameterDescriptor.getUpperBounds());
            }
            forceResolveAllContents(callableDescriptor.getReturnType());
        }
    }

    @Nullable
    public static JetType forceResolveAllContents(@Nullable JetType type) {
        if (type == null) return null;

        if (TypesPackage.isFlexible(type)) {
            forceResolveAllContents(TypesPackage.flexibility(type).getLowerBound());
            forceResolveAllContents(TypesPackage.flexibility(type).getUpperBound());
        }
        else {
            forceResolveAllContents(type.getConstructor());
            for (TypeProjection projection : type.getArguments()) {
                if (!projection.isStarProjection()) {
                    forceResolveAllContents(projection.getType());
                }
            }
        }
        return type;
    }
}
