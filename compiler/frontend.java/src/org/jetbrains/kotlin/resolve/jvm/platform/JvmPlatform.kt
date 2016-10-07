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

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleParameters
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.util.*

object JvmPlatform : TargetPlatform("JVM") {
    override val defaultModuleParameters = object : ModuleParameters {
        override val defaultImports: List<ImportPath>
            get() = DEFAULT_IMPORTS_FOR_JVM
    }

    override val platformConfigurator: PlatformConfigurator = JvmPlatformConfigurator
}

private val DEFAULT_IMPORTS_FOR_JVM: List<ImportPath> = ArrayList<ImportPath>().apply {
    add(ImportPath("java.lang.*"))
    add(ImportPath("kotlin.*"))
    add(ImportPath("kotlin.annotation.*"))
    add(ImportPath("kotlin.jvm.*"))
    add(ImportPath("kotlin.collections.*"))
    add(ImportPath("kotlin.coroutines.*"))
    add(ImportPath("kotlin.ranges.*"))
    add(ImportPath("kotlin.sequences.*"))
    add(ImportPath("kotlin.text.*"))
    add(ImportPath("kotlin.io.*"))

    fun addAllClassifiersFromScope(scope: MemberScope) {
        for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER)) {
            add(ImportPath(DescriptorUtils.getFqNameSafe(descriptor), false))
        }
    }

    val builtIns = DefaultBuiltIns.Instance
    for (builtinPackageFragment in builtIns.builtInsPackageFragments) {
        addAllClassifiersFromScope(builtinPackageFragment.getMemberScope())
    }
}
