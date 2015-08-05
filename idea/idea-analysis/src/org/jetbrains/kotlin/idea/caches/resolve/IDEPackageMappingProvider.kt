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
import org.jetbrains.kotlin.idea.vfilefinder.KotlinModuleMappingIndex
import org.jetbrains.kotlin.load.java.lazy.PackageMappingProvider
import org.jetbrains.kotlin.load.kotlin.PackageFacades

public class IDEPackageMappingProvider(val scope: GlobalSearchScope) : PackageMappingProvider {

    override fun findPackageMembers(packageName: String): List<String> {
        val values: MutableList<PackageFacades> = FileBasedIndex.getInstance().getValues(KotlinModuleMappingIndex.KEY, packageName, scope)
        return values.flatMap { it.parts }.distinct()
    }
}