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

import org.jetbrains.kotlin.descriptors.PlatformKind
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.util.*

object JvmPlatform : TargetPlatform("JVM") {
    override val defaultImports: List<ImportPath> = ArrayList<ImportPath>().apply {
        addAll(Default.defaultImports)

        add(ImportPath("java.lang.*"))
        add(ImportPath("kotlin.jvm.*"))
        add(ImportPath("kotlin.io.*"))

        fun addAllClassifiersFromScope(scope: MemberScope) {
            for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER)) {
                add(ImportPath(DescriptorUtils.getFqNameSafe(descriptor), false))
            }
        }

        val builtIns = JvmBuiltIns(LockBasedStorageManager.NO_LOCKS)
        for (builtinPackageFragment in builtIns.builtInsPackageFragmentsImportedByDefault) {
            addAllClassifiersFromScope(builtinPackageFragment.getMemberScope())
        }
    }

    override val platformConfigurator: PlatformConfigurator = JvmPlatformConfigurator

    override val kind: PlatformKind
        get() = PlatformKind.JVM
}
