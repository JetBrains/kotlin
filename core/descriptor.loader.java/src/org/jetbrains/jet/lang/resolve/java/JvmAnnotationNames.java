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

import jet.KotlinClass;
import jet.KotlinPackage;
import jet.KotlinPackageFragment;
import jet.KotlinTraitImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;

import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.fqNameByClass;

public final class JvmAnnotationNames {
    public static final FqName KOTLIN_CLASS = fqNameByClass(KotlinClass.class);

    public static final FqName KOTLIN_PACKAGE = fqNameByClass(KotlinPackage.class);

    public static final FqName KOTLIN_PACKAGE_FRAGMENT = fqNameByClass(KotlinPackageFragment.class);

    public static final FqName KOTLIN_TRAIT_IMPL = fqNameByClass(KotlinTraitImpl.class);

    public static final String ABI_VERSION_FIELD_NAME = "abiVersion";

    public static final String DATA_FIELD_NAME = "data";

    @Deprecated
    public static final FqName OLD_JET_CLASS_ANNOTATION = new FqName("jet.runtime.typeinfo.JetClass");

    @Deprecated
    public static final FqName OLD_JET_PACKAGE_CLASS_ANNOTATION = new FqName("jet.runtime.typeinfo.JetPackageClass");

    private JvmAnnotationNames() {
    }
}
