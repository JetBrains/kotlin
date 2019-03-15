/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.vfilefinder.KotlinPackageSourcesMemberNamesIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.safeNameForLazyResolve
import org.jetbrains.kotlin.resolve.lazy.data.KtClassInfoUtil
import org.jetbrains.kotlin.resolve.lazy.data.KtClassOrObjectInfo
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import java.util.*

class StubBasedPackageMemberDeclarationProvider(
        private val fqName: FqName,
        private val project: Project,
        private val searchScope: GlobalSearchScope
) : PackageMemberDeclarationProvider {

    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<KtDeclaration> {
        val result = ArrayList<KtDeclaration>()

        fun addFromIndex(index: StringStubIndexExtension<out KtNamedDeclaration>) {
            index.get(fqName.asString(), project, searchScope).filterTo(result) { nameFilter(it.nameAsSafeName) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            addFromIndex(KotlinTopLevelClassByPackageIndex.getInstance())
            addFromIndex(KotlinTopLevelTypeAliasByPackageIndex.getInstance())
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            addFromIndex(KotlinTopLevelFunctionByPackageIndex.getInstance())
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            addFromIndex(KotlinTopLevelPropertyByPackageIndex.getInstance())
        }

        return result
    }

    private val declarationNames_: Set<Name> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FileBasedIndex.getInstance()
                .getValues(KotlinPackageSourcesMemberNamesIndex.KEY, fqName.asString(), searchScope)
                .flatMapTo(hashSetOf()) {
                    it.map { stringName -> Name.identifier(stringName).safeNameForLazyResolve() }
                }
    }

    override fun getDeclarationNames() = declarationNames_

    override fun getClassOrObjectDeclarations(name: Name): Collection<KtClassOrObjectInfo<*>> = runReadAction {
        KotlinFullClassNameIndex.getInstance().get(childName(name), project, searchScope)
            .map { KtClassInfoUtil.createClassLikeInfo(it) }
    }

    override fun getScriptDeclarations(name: Name): Collection<KtScriptInfo> = runReadAction {
        KotlinScriptFqnIndex.instance.get(childName(name), project, searchScope)
            .map(::KtScriptInfo)
    }


    override fun getFunctionDeclarations(name: Name): Collection<KtNamedFunction> {
        return runReadAction {
            KotlinTopLevelFunctionFqnNameIndex.getInstance().get(childName(name), project, searchScope)
        }
    }

    override fun getPropertyDeclarations(name: Name): Collection<KtProperty> {
        return runReadAction {
            KotlinTopLevelPropertyFqnNameIndex.getInstance().get(childName(name), project, searchScope)
        }
    }

    override fun getDestructuringDeclarationsEntries(name: Name): Collection<KtDestructuringDeclarationEntry> {
        return emptyList()
    }

    override fun getAllDeclaredSubPackages(nameFilter: (Name) -> Boolean): Collection<FqName> {
        return PackageIndexUtil.getSubPackageFqNames(fqName, searchScope, project, nameFilter)
    }

    override fun getPackageFiles(): Collection<KtFile> {
        return PackageIndexUtil.findFilesWithExactPackage(fqName, searchScope, project)
    }

    override fun containsFile(file: KtFile): Boolean {
        return searchScope.contains(file.virtualFile ?: return false)
    }

    override fun getTypeAliasDeclarations(name: Name): Collection<KtTypeAlias> {
        return KotlinTopLevelTypeAliasFqNameIndex.getInstance().get(childName(name), project, searchScope)
    }

    private fun childName(name: Name): String {
        return fqName.child(name.safeNameForLazyResolve()).asString()
    }
}
