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

package org.jetbrains.kotlin.idea.stubindex.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.idea.stubindex.JetFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.data.JetClassInfoUtil
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import java.util.ArrayList
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionByPackageIndex
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelPropertyByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelPropertyFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelClassByPackageIndex

public class StubBasedPackageMemberDeclarationProvider(
        private val fqName: FqName,
        private val project: Project,
        private val searchScope: GlobalSearchScope
) : PackageMemberDeclarationProvider {

    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<JetDeclaration> {
        val result = ArrayList<JetDeclaration>()

        fun addFromIndex(index: StringStubIndexExtension<out JetNamedDeclaration>) {
            index.get(fqName.asString(), project, searchScope).filterTo(result) { nameFilter(it.getNameAsSafeName()) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            addFromIndex(JetTopLevelClassByPackageIndex.getInstance())
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            addFromIndex(JetTopLevelFunctionByPackageIndex.getInstance())
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            addFromIndex(JetTopLevelPropertyByPackageIndex.getInstance())
        }

        return result
    }

    override fun getClassOrObjectDeclarations(name: Name): Collection<JetClassLikeInfo> {
        return JetFullClassNameIndex.getInstance().get(childName(name), project, searchScope)
                .map { JetClassInfoUtil.createClassLikeInfo(it) }
    }

    override fun getFunctionDeclarations(name: Name): Collection<JetNamedFunction> {
        return JetTopLevelFunctionFqnNameIndex.getInstance().get(childName(name), project, searchScope)
    }

    override fun getPropertyDeclarations(name: Name): Collection<JetProperty> {
        return JetTopLevelPropertyFqnNameIndex.getInstance().get(childName(name), project, searchScope)
    }

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> {
        return PackageIndexUtil.getSubPackageFqNames(fqName, searchScope, project, nameFilter)
    }

    override fun getPackageFiles(): Collection<JetFile> {
       return PackageIndexUtil.findFilesWithExactPackage(fqName, searchScope, project)
    }

    private fun childName(name: Name): String {
        return fqName.child(ResolveSessionUtils.safeNameForLazyResolve(name)).asString()
    }
}
