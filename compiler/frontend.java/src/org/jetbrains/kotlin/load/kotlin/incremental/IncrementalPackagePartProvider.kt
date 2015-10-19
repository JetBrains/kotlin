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

package org.jetbrains.kotlin.load.kotlin.incremental

import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.storage.StorageManager

internal class IncrementalPackagePartProvider private constructor(
        private val parent: PackagePartProvider,
        sourceFiles: Collection<KtFile>,
        incrementalCaches: List<IncrementalCache>,
        storageManager: StorageManager
) : PackagePartProvider {
    private val moduleMappings = storageManager.createLazyValue { incrementalCaches.map { ModuleMapping.create(it.getModuleMappingData()) } }
    private val fqNamesToIgnore =
            incrementalCaches.flatMap { IncrementalPackageFragmentProvider.fqNamesToLoad(it.getObsoletePackageParts(), sourceFiles).map { it.asString() } }

    override fun findPackageParts(packageFqName: String): List<String> {
        val packagePartsFromParent = parent.findPackageParts(packageFqName)
        if (packageFqName in fqNamesToIgnore) return packagePartsFromParent

        val packagePartsFromCompiled = moduleMappings().map { it.findPackageParts(packageFqName) }.filterNotNull().flatMap { it.parts }
        return (packagePartsFromCompiled + packagePartsFromParent).distinct()
    }

    companion object {
        @JvmStatic
        public fun create(
                parent: PackagePartProvider,
                sourceFiles: Collection<KtFile>,
                targets: List<TargetId>?,
                incrementalCompilationComponents: IncrementalCompilationComponents?,
                storageManager: StorageManager
        ): PackagePartProvider {
            if (targets == null || incrementalCompilationComponents == null) return parent

            val incrementalCaches = targets.map { incrementalCompilationComponents.getIncrementalCache(it) }
            return IncrementalPackagePartProvider(parent, sourceFiles, incrementalCaches, storageManager)
        }
    }
}
