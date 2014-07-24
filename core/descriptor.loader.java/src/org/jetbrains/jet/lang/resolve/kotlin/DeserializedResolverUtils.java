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

package org.jetbrains.jet.lang.resolve.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.SpecialNames;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.name.SpecialNames.isClassObjectName;

public class DeserializedResolverUtils {
    private DeserializedResolverUtils() {
    }

    @NotNull
    public static FqName kotlinFqNameToJavaFqName(@NotNull FqNameUnsafe kotlinFqName) {
        List<Name> segments = kotlinFqName.pathSegments();
        List<String> correctedSegments = new ArrayList<String>(segments.size());
        for (Name segment : segments) {
            correctedSegments.add(isClassObjectName(segment) ? JvmAbi.CLASS_OBJECT_CLASS_NAME : segment.getIdentifier());
        }
        return FqName.fromSegments(correctedSegments);
    }

    @NotNull
    public static FqNameUnsafe javaFqNameToKotlinFqName(@NotNull FqName javaFqName) {
        if (javaFqName.isRoot()) {
            return javaFqName.toUnsafe();
        }
        List<Name> segments = javaFqName.pathSegments();
        List<Name> correctedSegments = new ArrayList<Name>(segments.size());
        correctedSegments.add(segments.get(0));
        for (int i = 1; i < segments.size(); i++) {
            Name segment = segments.get(i);
            boolean isClassObjectName = segment.asString().equals(JvmAbi.CLASS_OBJECT_CLASS_NAME);
            Name correctedSegment = isClassObjectName ? SpecialNames.getClassObjectName(segments.get(i - 1)) : segment;
            correctedSegments.add(correctedSegment);
        }
        return FqNameUnsafe.fromSegments(correctedSegments);
    }

    @NotNull
    public static FqNameUnsafe naiveKotlinFqName(@NotNull ClassDescriptor descriptor) {
        DeclarationDescriptor containing = descriptor.getContainingDeclaration();
        if (containing instanceof ClassDescriptor) {
            return naiveKotlinFqName((ClassDescriptor) containing).child(descriptor.getName());
        }
        else if (containing instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) containing).getFqName().child(descriptor.getName()).toUnsafe();
        }
        else {
            throw new IllegalArgumentException("Class doesn't have a FQ name: " + descriptor);
        }
    }
}
