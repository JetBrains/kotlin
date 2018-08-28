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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.vfilefinder.KotlinJvmModuleAnnotationsIndex
import org.jetbrains.kotlin.idea.vfilefinder.KotlinModuleMappingIndex
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider

class IDEPackagePartProvider(val scope: GlobalSearchScope) : PackagePartProvider, MetadataPartProvider {
    override fun findPackageParts(packageFqName: String): List<String> =
        getPackageParts(packageFqName).flatMap(PackageParts::parts).distinct()

    override fun findMetadataPackageParts(packageFqName: String): List<String> =
        getPackageParts(packageFqName).flatMap(PackageParts::metadataParts).distinct()

    private fun getPackageParts(packageFqName: String): MutableList<PackageParts> =
        FileBasedIndex.getInstance().getValues(KotlinModuleMappingIndex.KEY, packageFqName, scope)

    // Note that in case of several modules with the same name, we return all annotations on all of them, which is probably incorrect
    override fun getAnnotationsOnBinaryModule(moduleName: String): List<ClassId> =
        FileBasedIndex.getInstance().getValues(KotlinJvmModuleAnnotationsIndex.KEY, moduleName, scope).flatten()
}
