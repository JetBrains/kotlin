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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.find.findUsages.JavaMethodFindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.find.findUsages.JavaClassFindUsagesOptions
import org.jetbrains.jet.plugin.search.usagesSearch.ClassUsagesSearchHelper
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchTarget
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchLocation
import com.intellij.find.findUsages.FindUsagesOptions
import org.jetbrains.jet.plugin.search.usagesSearch.PropertyUsagesSearchHelper
import com.intellij.find.findUsages.JavaFindUsagesOptions
import org.jetbrains.jet.plugin.search.usagesSearch.DeclarationUsagesSearchHelper
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.plugin.search.usagesSearch.ClassDeclarationsUsagesSearchHelper
import org.jetbrains.jet.plugin.search.usagesSearch.FunctionUsagesSearchHelper
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchHelper
import kotlin.test.fail
import com.intellij.find.findUsages.JavaVariableFindUsagesOptions

public class KotlinClassFindUsagesOptions(project: Project) : JavaClassFindUsagesOptions(project) {
    public var searchConstructorUsages: Boolean = true

    public override fun equals(o: Any?): Boolean {
        return super.equals(o) && o is KotlinClassFindUsagesOptions && o.searchConstructorUsages == searchConstructorUsages
    }

    public override fun hashCode(): Int {
        return 31 * super.hashCode() + if (searchConstructorUsages) 1 else 0
    }
}

public trait KotlinCallableFindUsagesOptions {
    public var searchOverrides: Boolean
}

public class KotlinFunctionFindUsagesOptions(project: Project): KotlinCallableFindUsagesOptions, JavaMethodFindUsagesOptions(project) {
    override var searchOverrides: Boolean
        get() = isOverridingMethods
        set(value: Boolean) {
            isOverridingMethods = value
        }
}

public class KotlinPropertyFindUsagesOptions(project: Project): KotlinCallableFindUsagesOptions, JavaVariableFindUsagesOptions(project) {
    override var searchOverrides: Boolean = false
}

fun KotlinClassFindUsagesOptions.toClassHelper(): ClassUsagesSearchHelper =
        ClassUsagesSearchHelper().let { builder ->
            builder.constructorUsages = searchConstructorUsages
            builder.nonConstructorUsages = isUsages
            builder.skipImports = isSkipImportStatements

            builder
        }

fun KotlinClassFindUsagesOptions.toClassDeclarationsHelper(): ClassDeclarationsUsagesSearchHelper =
        ClassDeclarationsUsagesSearchHelper().let { builder ->
            builder.functionUsages = isMethodsUsages
            builder.propertyUsages = isFieldsUsages
            builder.skipImports = isSkipImportStatements

            builder
        }

fun KotlinFunctionFindUsagesOptions.toHelper(): FunctionUsagesSearchHelper =
        FunctionUsagesSearchHelper().let { builder ->
            builder.selfUsages = isUsages
            builder.overrideUsages = isUsages
            builder.overloadUsages = isIncludeOverloadUsages
            builder.extensionUsages = isIncludeOverloadUsages
            builder.skipImports = isSkipImportStatements

            builder
        }

fun KotlinPropertyFindUsagesOptions.toHelper(): PropertyUsagesSearchHelper =
        PropertyUsagesSearchHelper().let { builder ->
            builder.selfUsages = isUsages
            builder.overrideUsages = isUsages
            builder.readUsages = isReadAccess
            builder.writeUsages = isWriteAccess
            builder.skipImports = isSkipImportStatements

            builder
        }

fun JavaFindUsagesOptions.toHelper(): DeclarationUsagesSearchHelper<PsiNamedElement> =
        DeclarationUsagesSearchHelper<PsiNamedElement>().let { builder ->
            builder.skipImports = isSkipImportStatements

            builder
        }

fun <T : PsiNamedElement> FindUsagesOptions.toSearchTarget(element: T, restrictByTarget: Boolean): UsagesSearchTarget<T> {
    val location = if (isSearchForTextOccurrences && searchScope is GlobalSearchScope)
        UsagesSearchLocation.EVERYWHERE
    else
        UsagesSearchLocation.DEFAULT

    return UsagesSearchTarget(element, searchScope ?: element.getUseScope(), location, restrictByTarget)
}
