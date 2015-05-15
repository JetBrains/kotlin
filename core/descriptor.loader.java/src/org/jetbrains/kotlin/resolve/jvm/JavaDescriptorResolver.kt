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

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement

public var PLATFORM_TYPES: Boolean = true

public class JavaDescriptorResolver(public val packageFragmentProvider: LazyJavaPackageFragmentProvider, private val module: ModuleDescriptor) {

    public fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
        return packageFragmentProvider.getClass(javaClass)
    }
}