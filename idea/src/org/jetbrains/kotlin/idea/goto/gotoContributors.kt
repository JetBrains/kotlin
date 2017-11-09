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
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import java.util.*

class KotlinGotoClassContributor : GotoClassContributor {
    override fun getQualifiedName(item: NavigationItem): String? {
        val declaration = item as? KtNamedDeclaration ?: return null
        return declaration.fqName?.asString()
    }

    override fun getQualifiedNameSeparator() = "."

    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        val classes = KotlinClassShortNameIndex.getInstance().getAllKeys(project)
        val typeAliases = KotlinTypeAliasShortNameIndex.getInstance().getAllKeys(project)
        return (classes + typeAliases).toTypedArray()
    }

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        val globalScope = if (includeNonProjectItems) GlobalSearchScope.allScope(project) else GlobalSearchScope.projectScope(project)
        val scope = KotlinSourceFilterScope.projectSourceAndClassFiles(globalScope, project)
        val classesOrObjects = KotlinClassShortNameIndex.getInstance().get(name, project, scope)
        val typeAliases = KotlinTypeAliasShortNameIndex.getInstance().get(name, project, scope)

        if (classesOrObjects.isEmpty() && typeAliases.isEmpty()) return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY

        return (classesOrObjects + typeAliases).filter { it != null && it !is KtEnumEntry }.toTypedArray()
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
                KotlinClassShortNameIndex.getInstance(),
                KotlinTypeAliasShortNameIndex.getInstance()
        ).flatMap {
            StubIndex.getInstance().getAllKeys(it.key, project)
        }.toTypedArray()
    }

    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<NavigationItem> {
        val baseScope = if (includeNonProjectItems) GlobalSearchScope.allScope(project) else GlobalSearchScope.projectScope(project)
        val noLibrarySourceScope = KotlinSourceFilterScope.projectSourceAndClassFiles(baseScope, project)

        val result = ArrayList<NavigationItem>()
        result += KotlinFunctionShortNameIndex.getInstance().get(name, project, noLibrarySourceScope).filter {
            val method = LightClassUtil.getLightClassMethod(it)
            method == null || it.name != method.name
        }
        result += KotlinPropertyShortNameIndex.getInstance().get(name, project, noLibrarySourceScope).filter {
            LightClassUtil.getLightClassBackingField(it) == null ||
            it.containingClass()?.isInterface() ?: false
        }
        result += KotlinClassShortNameIndex.getInstance().get(name, project, noLibrarySourceScope).filter {
            it is KtEnumEntry || it.containingFile.virtualFile?.fileType == KotlinBuiltInFileType
        }
        result += KotlinTypeAliasShortNameIndex.getInstance().get(name, project, noLibrarySourceScope)

        return result.toTypedArray()
    }
}
