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

package org.jetbrains.kotlin.idea.goto

import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.stubindex.KotlinClassShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.*

class KotlinGotoClassContributor : GotoClassContributor {
    override fun getQualifiedName(item: NavigationItem): String? {
        val declaration = item as? KtNamedDeclaration ?: return null
        return declaration.fqName?.asString()
    }

    override fun getQualifiedNameSeparator() = "."

    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        return KotlinClassShortNameIndex.getInstance().getAllKeys(project).toTypedArray()
    }

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        val scope = if (includeNonProjectItems) GlobalSearchScope.allScope(project) else GlobalSearchScope.projectScope(project)
        val classesOrObjects = KotlinClassShortNameIndex.getInstance().get(name, project, KotlinSourceFilterScope.sourceAndClassFiles(scope, project))

        if (classesOrObjects.isEmpty()) return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY

        return classesOrObjects.filter { it != null && it !is KtEnumEntry }.toTypedArray()
    }
}

/*
* Logic in IDEA that adds classes to "go to symbol" popup result goes around GotoClassContributor.
* For Kotlin classes it works using light class generation.
* We have to process Kotlin builtIn classes separately since no light classes are built for them.
* */
class KotlinGotoSymbolContributor : ChooseByNameContributor {
    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        return listOf(
                KotlinFunctionShortNameIndex.getInstance(),
                KotlinPropertyShortNameIndex.getInstance(),
                KotlinClassShortNameIndex.getInstance()
        ).flatMap {
            StubIndex.getInstance().getAllKeys(it.key, project)
        }.toTypedArray()
    }

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        val baseScope = if (includeNonProjectItems) GlobalSearchScope.allScope(project) else GlobalSearchScope.projectScope(project)
        val noLibrarySourceScope = KotlinSourceFilterScope.sourceAndClassFiles(baseScope, project)

        val result = ArrayList<NavigationItem>()
        result += KotlinFunctionShortNameIndex.getInstance().get(name, project, noLibrarySourceScope)
        result += KotlinPropertyShortNameIndex.getInstance().get(name, project, noLibrarySourceScope)
        result += KotlinClassShortNameIndex.getInstance().get(name, project, BuiltInClassesScope(noLibrarySourceScope))

        return result.toTypedArray()
    }
}

private class BuiltInClassesScope(baseScope: GlobalSearchScope) : DelegatingGlobalSearchScope(baseScope) {
    override fun contains(file: VirtualFile): Boolean {
        return file.fileType == KotlinBuiltInFileType && file in myBaseScope
    }
}
