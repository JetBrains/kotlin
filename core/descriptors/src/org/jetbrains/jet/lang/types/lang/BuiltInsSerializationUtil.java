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

package org.jetbrains.jet.lang.types.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.name.SpecialNames.isClassObjectName;

public class BuiltInsSerializationUtil {
    private static final String CLASS_METADATA_FILE_EXTENSION = "kotlin_class";
    private static final String PACKAGE_FILE_NAME = ".kotlin_package";
    private static final String NAME_TABLE_FILE_NAME = ".kotlin_name_table";
    private static final String CLASS_NAMES_FILE_NAME = ".kotlin_class_names";
    private static final String CLASS_OBJECT_NAME = "object";

    private BuiltInsSerializationUtil() {
    }

    @NotNull
    public static String relativeClassNameToFilePath(@NotNull FqNameUnsafe className) {
        List<Name> segments = className.pathSegments();
        List<String> correctedSegments = new ArrayList<String>(segments.size());
        for (Name segment : segments) {
            correctedSegments.add(isClassObjectName(segment) ? CLASS_OBJECT_NAME : segment.getIdentifier());
        }
        return FqName.fromSegments(correctedSegments).asString();
    }

    @NotNull
    public static String getClassMetadataPath(@NotNull ClassId classId) {
        return packageFqNameToPath(classId.getPackageFqName())
               + "/" + relativeClassNameToFilePath(classId.getRelativeClassName())
               + "." + CLASS_METADATA_FILE_EXTENSION;
    }

    @NotNull
    public static String getPackageFilePath(@NotNull PackageFragmentDescriptor packageFragment) {
        FqName fqName = packageFragment.getFqName();
        return packageFqNameToPath(fqName) + "/" + PACKAGE_FILE_NAME;
    }

    @NotNull
    public static String getNameTableFilePath(@NotNull PackageFragmentDescriptor packageFragment) {
        FqName fqName = packageFragment.getFqName();
        return packageFqNameToPath(fqName) + "/" + NAME_TABLE_FILE_NAME;
    }

    @NotNull
    public static String getClassNamesFilePath(@NotNull PackageFragmentDescriptor packageFragmentDescriptor) {
        FqName fqName = packageFragmentDescriptor.getFqName();
        return packageFqNameToPath(fqName) + "/" + CLASS_NAMES_FILE_NAME;
    }

    private static String packageFqNameToPath(FqName fqName) {
        return fqName.asString().replace('.', '/');
    }
}
