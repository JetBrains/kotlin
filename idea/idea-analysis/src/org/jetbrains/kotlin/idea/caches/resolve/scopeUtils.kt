/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile

//NOTE: idea default API returns module search scope for file under module but not in source or production source (for example, test data )
// this scope can't be used to search for kotlin declarations in index in order to resolve in that case
// see com.intellij.psi.impl.file.impl.ResolveScopeManagerImpl.getInherentResolveScope
fun getResolveScope(file: KtFile): GlobalSearchScope {
    if (file is KtCodeFragment) {
        // scope should be corrected when KT-6223 is implemented
        file.getContextContainingFile()?.resolveScope?.let {
            return when (file.getModuleInfo()) {
                is SourceForBinaryModuleInfo -> KotlinSourceFilterScope.libraryClassFiles(it, file.project)
                else -> KotlinSourceFilterScope.sourceAndClassFiles(it, file.project)
            }
        }
    }

    return when (file.getModuleInfo()) {
        is ModuleSourceInfo -> KotlinSourceFilterScope.projectSourceAndClassFiles(file.resolveScope, file.project)
        is ScriptModuleInfo -> file.getModuleInfo().dependencies().map { it.contentScope() }.let { GlobalSearchScope.union(it.toTypedArray()) }
        else -> GlobalSearchScope.EMPTY_SCOPE
    }
}
