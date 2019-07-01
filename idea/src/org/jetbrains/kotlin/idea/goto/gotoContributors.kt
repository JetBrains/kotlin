/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.goto

import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInFileType
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtUserType
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
class KotlinGotoSymbolContributor : GotoClassContributor {
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

    override fun getQualifiedName(item: NavigationItem): String? {
        if (item is KtCallableDeclaration) {
            val receiverType = (item.receiverTypeReference?.typeElement as? KtUserType)?.referencedName
            if (receiverType != null) {
                return "$receiverType.${item.name}"
            }
        }
        return null
    }

    override fun getQualifiedNameSeparator(): String = "."
}
