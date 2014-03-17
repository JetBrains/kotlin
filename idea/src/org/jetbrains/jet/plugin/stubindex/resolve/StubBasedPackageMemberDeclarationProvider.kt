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

package org.jetbrains.jet.plugin.stubindex.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.plugin.stubindex.JetAllPackagesIndex
import org.jetbrains.jet.plugin.stubindex.JetFullClassNameIndex
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex
import org.jetbrains.jet.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex
import java.util.*
import org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope.kotlinSources
import org.jetbrains.jet.lang.resolve.name.numberOfSegments

public class StubBasedPackageMemberDeclarationProvider(
        private val fqName: FqName,
        private val project: Project,
        private val searchScope: GlobalSearchScope
) : PackageMemberDeclarationProvider {

    override fun getAllDeclarations(): List<JetDeclaration> {
        return TOP_LEVEL_DECLARATION_INDICES.flatMap {
            index ->
            val fqNames = index.getAllKeys(project).toSet().map { FqName(it) }.filter { !it.isRoot() && it.parent() == fqName }
            fqNames.flatMap { index.get(it.asString(), project, searchScope) }
        }
    }

    override fun getClassOrObjectDeclarations(name: Name): Collection<JetClassOrObject> {
        return JetFullClassNameIndex.getInstance().get(childName(name), project, searchScope)
    }

    override fun getFunctionDeclarations(name: Name): Collection<JetNamedFunction> {
        return JetTopLevelFunctionsFqnNameIndex.getInstance().get(childName(name), project, searchScope)
    }

    override fun getPropertyDeclarations(name: Name): Collection<JetProperty> {
        return JetTopLevelPropertiesFqnNameIndex.getInstance().get(childName(name), project, searchScope)
    }

    override fun getAllDeclaredPackages(): Collection<FqName> {
        //TODO: duplication with light class generation support
        val allPackagesInProject = JetAllPackagesIndex.getInstance().getAllKeys(project)
        return allPackagesInProject.filter {
            val otherPackageFqName = FqName(it)
            !otherPackageFqName.isRoot() && otherPackageFqName.parent() == fqName
        }.map { FqName(it) }
    }

    override fun getPackageDeclarations(fqName: FqName): Collection<NavigatablePsiElement> {
        if (fqName.isRoot()) {
            return Collections.emptyList()
        }

        val files = JetAllPackagesIndex.getInstance().get(fqName.asString(), project, searchScope)
        return files.map {
            file ->
            JetPsiUtil.getPackageReference(file, fqName.numberOfSegments() - 1)
        }.filterNotNull()
    }

    override fun getPackageFiles(): Collection<JetFile> {
        //TODO: duplicate with light class generation support
        val files = JetAllPackagesIndex.getInstance().get(fqName.asString(), project, kotlinSources(searchScope))
        return files.filter {
            fqName.equals(JetPsiUtil.getFQName(it))
        }
    }

    private fun childName(name: Name): String {
        return fqName.child(name).asString()
    }
}

private val TOP_LEVEL_DECLARATION_INDICES: List<StringStubIndexExtension<out JetNamedDeclaration>> = listOf(
        JetFullClassNameIndex.getInstance(),
        JetTopLevelFunctionsFqnNameIndex.getInstance(),
        JetTopLevelPropertiesFqnNameIndex.getInstance()
)