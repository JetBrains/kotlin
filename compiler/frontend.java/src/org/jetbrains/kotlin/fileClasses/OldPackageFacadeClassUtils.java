/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.fileClasses;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;

public final class OldPackageFacadeClassUtils {
    private static final String PACKAGE_CLASS_NAME_SUFFIX = "Package";
    private static final String DEFAULT_PACKAGE_CLASS_NAME = "_Default" + PACKAGE_CLASS_NAME_SUFFIX;

    private OldPackageFacadeClassUtils() {
    }

    // ex. <root> -> _DefaultPackage, a -> APackage, a.b -> BPackage
    @NotNull
    public static String getPackageClassName(@NotNull FqName packageFQN) {
        if (packageFQN.isRoot()) {
            return DEFAULT_PACKAGE_CLASS_NAME;
        }
        return capitalizeNonEmptyString(packageFQN.shortName().asString()) + PACKAGE_CLASS_NAME_SUFFIX;
    }

    @NotNull
    private static String capitalizeNonEmptyString(@NotNull String s) {
        return Character.isUpperCase(s.charAt(0)) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
