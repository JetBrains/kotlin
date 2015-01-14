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

package org.jetbrains.kotlin.load.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;

import java.util.ArrayList;
import java.util.List;

public class DeserializedResolverUtils {
    private DeserializedResolverUtils() {
    }

    //TODO_R: remove usages
    @NotNull
    public static FqName kotlinFqNameToJavaFqName(@NotNull FqNameUnsafe kotlinFqName) {
        List<Name> segments = kotlinFqName.pathSegments();
        List<String> correctedSegments = new ArrayList<String>(segments.size());
        for (Name segment : segments) {
            correctedSegments.add(segment.getIdentifier());
        }
        return FqName.fromSegments(correctedSegments);
    }

    @NotNull
    public static ClassId kotlinClassIdToJavaClassId(@NotNull ClassId kotlinClassId) {
        return new ClassId(kotlinClassId.getPackageFqName(), kotlinFqNameToJavaFqName(kotlinClassId.getRelativeClassName()).toUnsafe());
    }

    //TODO_R: remove usages
    @NotNull
    public static FqNameUnsafe javaFqNameToKotlinFqName(@NotNull FqName javaFqName) {
        if (javaFqName.isRoot()) {
            return javaFqName.toUnsafe();
        }
        List<Name> segments = javaFqName.pathSegments();
        List<Name> correctedSegments = new ArrayList<Name>(segments.size());
        correctedSegments.add(segments.get(0));
        for (int i = 1; i < segments.size(); i++) {
            correctedSegments.add(segments.get(i));
        }
        return FqNameUnsafe.fromSegments(correctedSegments);
    }

    //TODO_R: remove usages
    @NotNull
    public static ClassId javaClassIdToKotlinClassId(@NotNull ClassId javaClassId) {
        return new ClassId(javaClassId.getPackageFqName(), javaFqNameToKotlinFqName(javaClassId.getRelativeClassName().toSafe()));
    }

    @NotNull
    public static ClassId getClassId(@NotNull ClassDescriptor descriptor) {
        DeclarationDescriptor owner = descriptor.getContainingDeclaration();
        if (owner instanceof PackageFragmentDescriptor) {
            return new ClassId(((PackageFragmentDescriptor) owner).getFqName(), descriptor.getName());
        }
        else if (owner instanceof ClassDescriptor) {
            return getClassId((ClassDescriptor) owner).createNestedClassId(descriptor.getName());
        }
        else {
            throw new IllegalStateException("Illegal container: " + descriptor);
        }
    }
}
