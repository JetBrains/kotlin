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

package org.jetbrains.jet.plugin.stubindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.name.FqName
import kotlin.platform.platformStatic

public object PackageIndexUtil {
    platformStatic public fun getSubPackageFqNames(
            packageFqName: FqName,
            scope: GlobalSearchScope,
            project: Project
    ): Collection<FqName> {
        return SubpackagesIndexService.getInstance(project).getSubpackages(packageFqName, scope)
    }

    platformStatic public fun findFilesWithExactPackage(
            packageFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project
    ): Collection<JetFile> {
        return JetExactPackagesIndex.getInstance().get(packageFqName.asString(), project, searchScope)
    }

    platformStatic public fun packageExists(
            packageFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project
    ): Boolean {
        return containsAny(packageFqName, searchScope, project, JetExactPackagesIndex.getInstance().getKey()) ||
               SubpackagesIndexService.getInstance(project).hasSubpackages(packageFqName, searchScope)
    }

    platformStatic public fun containsAny(
            packageFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project,
            key: StubIndexKey<String, JetFile>
    ): Boolean {
        var result = false
        StubIndex.getInstance().processElements<String, JetFile>(key, packageFqName.asString(), project, searchScope, javaClass<JetFile>()) {
            result = true
            false
        }
        return result
    }
}
