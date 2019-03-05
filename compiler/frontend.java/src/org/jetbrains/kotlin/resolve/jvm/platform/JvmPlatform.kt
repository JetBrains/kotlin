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

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager

object JvmPlatform : TargetPlatform("JVM") {
    override val platform = MultiTargetPlatform.Specific(platformName)
}

object JvmPlatformCompilerServices : PlatformDependentCompilerServices() {
    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        result.add(ImportPath.fromString("kotlin.jvm.*"))

        fun addAllClassifiersFromScope(scope: MemberScope) {
            for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.ALL_NAME_FILTER)) {
                result.add(ImportPath(DescriptorUtils.getFqNameSafe(descriptor), false))
            }
        }

        for (builtInPackage in JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_CLASS_LOADER).builtInPackagesImportedByDefault) {
            addAllClassifiersFromScope(builtInPackage.memberScope)
        }
    }

    override val defaultLowPriorityImports: List<ImportPath> = listOf(ImportPath.fromString("java.lang.*"))

    override val platformConfigurator: PlatformConfigurator = JvmPlatformConfigurator
}
