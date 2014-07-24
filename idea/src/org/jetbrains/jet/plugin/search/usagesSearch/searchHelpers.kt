/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.search.usagesSearch

import com.intellij.psi.PsiReference
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.plugin.search.usagesSearch.*
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchFilter.*
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetNamedFunction
import java.util.Collections
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import com.intellij.psi.PsiNamedElement
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.jet.asJava.LightClassUtil
import com.intellij.psi.PsiMethod
import com.intellij.util.Query
import com.intellij.psi.search.SearchRequestCollector
import org.jetbrains.jet.lang.psi.JetCallableDeclaration
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.asJava.LightClassUtil.PropertyAccessorsPsiMethods
import org.jetbrains.jet.lang.psi.psiUtil.*

val isTargetUsage = (PsiReference::isTargetUsage).searchFilter

fun JetNamedDeclaration.namesWithAccessors(readable: Boolean = true, writable: Boolean = true): List<String> {
    val name = getName()!!

    fun PropertyAccessorsPsiMethods.toNameList(): List<String> {
        val getter = getGetter()
        val setter = getSetter()

        val result = arrayListOf(name)
        if (readable && getter != null) result.add(getter.getName())
        if (writable && setter != null) result.add(setter.getName())
        return result
    }

    if (JetPsiUtil.isLocal(this)) return Collections.singletonList(name)

    when (this) {
        is JetProperty ->
            return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
        is JetParameter ->
            if (hasValOrVarNode()) {
                return LightClassUtil.getLightClassPropertyMethods(this).toNameList()
            }
    }

    return Collections.singletonList(name)
}

public abstract class UsagesSearchHelper<T : PsiNamedElement> {
    protected open fun makeFilter(target: UsagesSearchTarget<T>): UsagesSearchFilter = isTargetUsage

    protected open fun makeWordList(target: UsagesSearchTarget<T>): List<String> {
        return with(target) {
            val name = element.getName()

            when {
                name == null -> Collections.emptyList<String>()
                element is JetProperty, element is JetParameter -> (element as JetNamedDeclaration).namesWithAccessors()
                else -> Collections.singletonList(name)
            }
        }
    }


    protected open fun makeItemList(target: UsagesSearchTarget<T>): List<UsagesSearchRequestItem> =
            Collections.singletonList(newItem(target))

    fun newItem(target: UsagesSearchTarget<T>): UsagesSearchRequestItem {
        return UsagesSearchRequestItem(target, makeWordList(target), makeFilter(target))
    }

    fun newRequest(target: UsagesSearchTarget<T>): UsagesSearchRequest {
        return UsagesSearchRequest(target.element.getProject(), makeItemList(target))
    }
}

val isNotImportUsage = !((PsiReference::isImportUsage).searchFilter)

trait ImportAwareSearchHelper {
    public val skipImports: Boolean

    protected val isFilteredImport: UsagesSearchFilter
        get() = isNotImportUsage.ifOrTrue(skipImports)
}

open class DefaultSearchHelper<T: PsiNamedElement>(
        override val skipImports: Boolean = false
): UsagesSearchHelper<T>(), ImportAwareSearchHelper {
    override fun makeFilter(target: UsagesSearchTarget<T>): UsagesSearchFilter = isTargetUsage and isFilteredImport
}

val isClassConstructorUsage = (PsiReference::isConstructorUsage).searchFilter

class ClassUsagesSearchHelper(
        public val constructorUsages: Boolean = false,
        public val nonConstructorUsages: Boolean = false,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetClassOrObject>(skipImports) {
    override fun makeFilter(target: UsagesSearchTarget<JetClassOrObject>): UsagesSearchFilter =
            super.makeFilter(target) and when {
                constructorUsages && !nonConstructorUsages -> isClassConstructorUsage
                !constructorUsages && nonConstructorUsages -> !isClassConstructorUsage
                !constructorUsages && !nonConstructorUsages -> False
                else -> True
            }
}

class ClassDeclarationsUsagesSearchHelper(
        public val functionUsages: Boolean = false,
        public val propertyUsages: Boolean = false,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetClassOrObject>(skipImports) {
    override fun makeItemList(target: UsagesSearchTarget<JetClassOrObject>): List<UsagesSearchRequestItem> {
        val items = ArrayList<UsagesSearchRequestItem>()
        val declHelper = DefaultSearchHelper<JetNamedDeclaration>(skipImports)

        for (decl in target.element.effectiveDeclarations()) {
            if ((decl is JetNamedFunction && functionUsages) || ((decl is JetProperty || decl is JetParameter) && propertyUsages)) {
                items.add(declHelper.newItem(target.retarget(decl as JetNamedDeclaration)))
            }
        }

        return items
    }
}

val isOverrideUsage = (PsiReference::isCallableOverrideUsage).searchFilter

trait OverrideSearchHelper {
    public val selfUsages: Boolean
    public val overrideUsages: Boolean

    val isTargetOrOverrideUsage: UsagesSearchFilter
        get() = isTargetUsage.ifOrFalse(selfUsages) or isOverrideUsage.ifOrFalse(overrideUsages)
}

val isOverloadUsage = (PsiReference::isUsageInContainingDeclaration).searchFilter
val isExtensionUsage = (PsiReference::isExtensionOfDeclarationClassUsage).searchFilter

class FunctionUsagesSearchHelper(
        public val overloadUsages: Boolean = false,
        public val extensionUsages: Boolean = false,
        override val selfUsages: Boolean = true,
        override val overrideUsages: Boolean = true,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetNamedFunction>(skipImports), OverrideSearchHelper {
    override fun makeFilter(target: UsagesSearchTarget<JetNamedFunction>): UsagesSearchFilter {
        return (isTargetOrOverrideUsage
        or isOverloadUsage.ifOrFalse(overloadUsages)
        or isExtensionUsage.ifOrFalse(extensionUsages)) and isFilteredImport
    }
}

val isPropertyReadOnlyUsage = (PsiReference::isPropertyReadOnlyUsage).searchFilter

// Used for JetProperty and JetParameter
class PropertyUsagesSearchHelper(
        public val readUsages: Boolean = true,
        public val writeUsages: Boolean = true,
        public override val selfUsages: Boolean = true,
        public override val overrideUsages: Boolean = true,
        skipImports: Boolean = false
) : DefaultSearchHelper<JetNamedDeclaration>(skipImports), OverrideSearchHelper {
    override fun makeWordList(target: UsagesSearchTarget<JetNamedDeclaration>): List<String> {
        return target.element.namesWithAccessors(readable = readUsages, writable = writeUsages)
    }

    override fun makeFilter(target: UsagesSearchTarget<JetNamedDeclaration>): UsagesSearchFilter {
        val readWriteUsage = when {
            readUsages && writeUsages -> True
            readUsages -> isPropertyReadOnlyUsage
            writeUsages -> !isPropertyReadOnlyUsage
            else -> False
        }
        return isTargetOrOverrideUsage and readWriteUsage and isFilteredImport
    }
}
