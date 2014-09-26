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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.ClassId;
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

    @Nullable
    private static String relativeClassNameToFilePath(@NotNull FqNameUnsafe className) {
        List<Name> segments = className.pathSegments();
        List<String> correctedSegments = new ArrayList<String>(segments.size());
        for (Name segment : segments) {
            if (isClassObjectName(segment)) {
                correctedSegments.add(CLASS_OBJECT_NAME);
            }
            else if (!segment.isSpecial()) {
                correctedSegments.add(segment.getIdentifier());
            }
            else return null;
        }
        return FqName.fromSegments(correctedSegments).asString();
    }

    @Nullable
    public static String getClassMetadataPath(@NotNull ClassId classId) {
        String filePath = relativeClassNameToFilePath(classId.getRelativeClassName());
        if (filePath == null) return null;
        return packageFqNameToPath(classId.getPackageFqName()) + "/" + filePath + "." + CLASS_METADATA_FILE_EXTENSION;
    }

    @NotNull
    public static String getPackageFilePath(@NotNull FqName fqName) {
        return packageFqNameToPath(fqName) + "/" + PACKAGE_FILE_NAME;
    }

    @NotNull
    public static String getNameTableFilePath(@NotNull FqName fqName) {
        return packageFqNameToPath(fqName) + "/" + NAME_TABLE_FILE_NAME;
    }

    @NotNull
    public static String getClassNamesFilePath(@NotNull FqName fqName) {
        return packageFqNameToPath(fqName) + "/" + CLASS_NAMES_FILE_NAME;
    }

    private static String packageFqNameToPath(FqName fqName) {
        return fqName.asString().replace('.', '/');
    }
}
