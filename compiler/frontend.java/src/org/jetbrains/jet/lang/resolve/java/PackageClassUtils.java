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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.kt.JetPackageClassAnnotation;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class PackageClassUtils {

    private static final String DEFAULT_PACKAGE = "_DefaultPackage";

    // ex. <root> -> _DefaultPackage, a -> APackage, a.b -> BPackage
    public static String getPackageClassName(@NotNull FqName packageFQN) {
        if (packageFQN.isRoot()) {
            return DEFAULT_PACKAGE;
        }
        return StringUtil.capitalize(packageFQN.shortName().asString()) + "Package";
    }

    public static FqName getPackageClassFqName(@NotNull FqName packageFQN) {
        return packageFQN.child(Name.identifier(getPackageClassName(packageFQN)));
    }

    public static boolean isPackageClass(@NotNull PsiClass psiClass) {
        return JetPackageClassAnnotation.get(psiClass).isDefined();
    }

    public static boolean isPackageClass(@NotNull FqName fqName) {
        if (fqName.isRoot()) {
            return false;
        }
        return PackageClassUtils.getPackageClassFqName(fqName.parent()).equals(fqName);
    }
}
