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
