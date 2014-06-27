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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public final class PackageClassUtils {
    public static final String PACKAGE_CLASS_NAME_SUFFIX = "Package";
    private static final String DEFAULT_PACKAGE_CLASS_NAME = "_Default" + PACKAGE_CLASS_NAME_SUFFIX;

    private PackageClassUtils() {
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

    @NotNull
    public static FqName getPackageClassFqName(@NotNull FqName packageFQN) {
        return packageFQN.child(Name.identifier(getPackageClassName(packageFQN)));
    }

    @NotNull
    public static String getPackageClassInternalName(@NotNull FqName packageFQN) {
        return JvmClassName.byFqNameWithoutInnerClasses(getPackageClassFqName(packageFQN)).getInternalName();
    }

    public static boolean isPackageClassFqName(@NotNull FqName fqName) {
        return !fqName.isRoot() && getPackageClassFqName(fqName.parent()).equals(fqName);
    }
}
