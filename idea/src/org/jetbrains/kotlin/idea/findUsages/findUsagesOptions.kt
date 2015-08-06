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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.findUsages.JavaClassFindUsagesOptions
import com.intellij.find.findUsages.JavaMethodFindUsagesOptions
import com.intellij.find.findUsages.JavaVariableFindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.search.usagesSearch.*

public class KotlinClassFindUsagesOptions(project: Project) : JavaClassFindUsagesOptions(project) {
    public var searchConstructorUsages: Boolean = true

    public override fun equals(o: Any?): Boolean {
        return super.equals(o) && o is KotlinClassFindUsagesOptions && o.searchConstructorUsages == searchConstructorUsages
    }

    public override fun hashCode(): Int {
        return 31 * super.hashCode() + if (searchConstructorUsages) 1 else 0
    }
}

public interface KotlinCallableFindUsagesOptions {
    public var searchOverrides: Boolean
}

public class KotlinFunctionFindUsagesOptions(project: Project): KotlinCallableFindUsagesOptions, JavaMethodFindUsagesOptions(project) {
    override var searchOverrides: Boolean
        get() = isOverridingMethods
        set(value: Boolean) {
            isOverridingMethods = value
        }
}

fun KotlinFunctionFindUsagesOptions.toJavaMethodOptions(project: Project): JavaMethodFindUsagesOptions {
    val javaOptions = JavaMethodFindUsagesOptions(project)
    javaOptions.fastTrack = fastTrack
    javaOptions.isCheckDeepInheritance = isCheckDeepInheritance
    javaOptions.isImplementingMethods = isImplementingMethods
    javaOptions.isIncludeInherited = isIncludeInherited
    javaOptions.isIncludeOverloadUsages = isIncludeOverloadUsages
    javaOptions.isOverridingMethods = isOverridingMethods
    javaOptions.isSearchForTextOccurrences = isSearchForTextOccurrences
    javaOptions.isSkipImportStatements = isSkipImportStatements
    javaOptions.isUsages = isUsages
    javaOptions.searchScope = searchScope
    
    return javaOptions
}

fun KotlinPropertyFindUsagesOptions.toJavaVariableOptions(project: Project): JavaVariableFindUsagesOptions {
    val javaOptions = JavaVariableFindUsagesOptions(project)
    javaOptions.fastTrack = fastTrack
    javaOptions.isSearchForTextOccurrences = isSearchForTextOccurrences
    javaOptions.isSkipImportStatements = isSkipImportStatements
    javaOptions.isReadAccess = isReadAccess
    javaOptions.isWriteAccess = isWriteAccess
    javaOptions.isUsages = isUsages
    javaOptions.searchScope = searchScope
    return javaOptions
}

public class KotlinPropertyFindUsagesOptions(project: Project): KotlinCallableFindUsagesOptions, JavaVariableFindUsagesOptions(project) {
    override var searchOverrides: Boolean = false
}

fun KotlinClassFindUsagesOptions.toClassHelper(): ClassUsagesSearchHelper =
        ClassUsagesSearchHelper(
            constructorUsages = searchConstructorUsages,
            nonConstructorUsages = isUsages,
            skipImports = isSkipImportStatements
        )

fun KotlinClassFindUsagesOptions.toClassDeclarationsHelper(): ClassDeclarationsUsagesSearchHelper =
        ClassDeclarationsUsagesSearchHelper(
            functionUsages = isMethodsUsages,
            propertyUsages = isFieldsUsages,
            skipImports = isSkipImportStatements
        )

fun KotlinFunctionFindUsagesOptions.toHelper(): FunctionUsagesSearchHelper =
        FunctionUsagesSearchHelper(
            selfUsages = isUsages,
            overrideUsages = isUsages,
            overloadUsages = isIncludeOverloadUsages,
            extensionUsages = isIncludeOverloadUsages,
            skipImports = isSkipImportStatements
        )

fun KotlinPropertyFindUsagesOptions.toHelper(): PropertyUsagesSearchHelper =
        PropertyUsagesSearchHelper(
            selfUsages = isUsages,
            overrideUsages = isUsages,
            namedArgumentUsages = isUsages,
            readUsages = isReadAccess,
            writeUsages = isWriteAccess,
            skipImports = isSkipImportStatements
        )

fun <T : PsiNamedElement> FindUsagesOptions.toSearchTarget(element: T, restrictByTarget: Boolean): UsagesSearchTarget<T> {
    val location = if (isSearchForTextOccurrences && searchScope is GlobalSearchScope)
        UsagesSearchLocation.EVERYWHERE
    else
        UsagesSearchLocation.DEFAULT

    return UsagesSearchTarget(element, searchScope, location, restrictByTarget)
}
