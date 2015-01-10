/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import kotlin.properties.Delegates

class LazyJavaPackageFragment(
        private val c: LazyJavaResolverContext,
        private val jPackage: JavaPackage
) : PackageFragmentDescriptorImpl(c.packageFragmentProvider.module, jPackage.getFqName()) {
    private val scope by Delegates.lazy { LazyPackageFragmentScopeForJavaPackage(c, jPackage, this) }

    override fun getMemberScope() = scope

    override fun toString() = "lazy java package fragment: $fqName"
}
