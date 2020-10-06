/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper.Companion.asStringForIndexes
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/*
* Move to another module
*/
public class IndexHelper(val project: Project, private val scope: GlobalSearchScope) {
    private val stubIndex: StubIndex = StubIndex.getInstance()

    private inline fun <INDEX_KEY : Any, reified PSI : PsiElement> firstMatchingOrNull(
        stubKey: StubIndexKey<INDEX_KEY, PSI>,
        key: INDEX_KEY,
        crossinline filter: (PSI) -> Boolean
    ): PSI? {
        var result: PSI? = null
        stubIndex.processElements(
            stubKey, key, project, scope, PSI::class.java,
        ) { candidate ->
            if (filter(candidate)) {
                result = candidate
                return@processElements false // do not continue searching over PSI
            }
            true
        }
        return result
    }

    fun classFromIndexByClassId(classId: ClassId) = firstMatchingOrNull(
        KotlinFullClassNameIndex.KEY,
        classId.asStringForIndexes(),
    ) { candidate -> candidate.containingKtFile.packageFqName == classId.packageFqName }

    fun typeAliasFromIndexByClassId(classId: ClassId) = firstMatchingOrNull(
        KotlinTopLevelTypeAliasFqNameIndex.KEY,
        classId.asStringForIndexes(),
    ) { candidate -> candidate.containingKtFile.packageFqName == classId.packageFqName }


    fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        KotlinTopLevelPropertyFqnNameIndex.getInstance()[callableId.asStringForIndexes(), project, scope]

    fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        KotlinTopLevelFunctionFqnNameIndex.getInstance()[callableId.asStringForIndexes(), project, scope]

    fun getTopLevelPropertiesInPackage(packageFqName: FqName): Collection<KtProperty> =
        KotlinTopLevelPropertyByPackageIndex.getInstance().get(packageFqName.asStringForIndexes(), project, scope)

    fun getTopLevelFunctionsInPackage(packageFqName: FqName): Collection<KtFunction> =
        KotlinTopLevelFunctionByPackageIndex.getInstance().get(packageFqName.asStringForIndexes(), project, scope)


    companion object {
        private fun CallableId.asStringForIndexes(): String =
            (if (packageName.isRoot) callableName.asString() else toString()).replace('/', '.')

        private fun FqName.asStringForIndexes(): String =
            asString().replace('/', '.')

        private fun ClassId.asStringForIndexes(): String =
            asSingleFqName().asStringForIndexes()
    }
}