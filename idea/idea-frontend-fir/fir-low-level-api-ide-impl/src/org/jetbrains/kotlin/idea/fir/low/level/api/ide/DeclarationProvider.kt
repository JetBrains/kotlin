/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.ide

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.KtDeclarationProviderFactory
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*


internal class DeclarationProviderByIndexesImpl(
    val project: Project,
    override val searchScope: GlobalSearchScope
) : DeclarationProvider() {

    private val stubIndex: StubIndex = StubIndex.getInstance()

    private inline fun <INDEX_KEY : Any, reified PSI : PsiElement> firstMatchingOrNull(
        stubKey: StubIndexKey<INDEX_KEY, PSI>,
        key: INDEX_KEY,
        crossinline filter: (PSI) -> Boolean = { true }
    ): PSI? {
        var result: PSI? = null
        stubIndex.processElements(
            stubKey, key, project, searchScope, PSI::class.java,
        ) { candidate ->
            if (filter(candidate)) {
                result = candidate
                return@processElements false // do not continue searching over PSI
            }
            true
        }
        return result
    }


    override fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        return listOfNotNull(getClassByClassId(classId))//todo
    }

    override fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        return listOfNotNull(getTypeAliasByClassId(classId))//todo
    }

    override fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name> {
        return KotlinTopLevelTypeAliasByPackageIndex
            .getInstance()[packageFqName.asStringForIndexes(), project, searchScope]
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
    }

    override fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name> {
        return KotlinTopLevelPropertyByPackageIndex
            .getInstance()[packageFqName.asStringForIndexes(), project, searchScope]
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
    }

    override fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name> {
        return KotlinTopLevelFunctionByPackageIndex
            .getInstance()[packageFqName.asStringForIndexes(), project, searchScope]
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
    }

    private fun getClassByClassId(classId: ClassId) = firstMatchingOrNull(
        KotlinFullClassNameIndex.KEY,
        classId.asStringForIndexes(),
    ) { candidate -> candidate.containingKtFile.packageFqName == classId.packageFqName }

    private fun getTypeAliasByClassId(classId: ClassId): KtTypeAlias? = firstMatchingOrNull<String, KtTypeAlias>(
        KotlinTopLevelTypeAliasFqNameIndex.KEY,
        key = classId.asStringForIndexes(),
    ) { candidate -> candidate.containingKtFile.packageFqName == classId.packageFqName }
        ?: firstMatchingOrNull<String, KtTypeAlias>(
            KotlinInnerTypeAliasClassIdIndex.KEY,
            key = classId.asString(),
        )

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        KotlinTopLevelPropertyFqnNameIndex.getInstance()[callableId.asStringForIndexes(), project, searchScope]

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        KotlinTopLevelFunctionFqnNameIndex.getInstance()[callableId.asStringForIndexes(), project, searchScope]


    override fun getClassNamesInPackage(packageFqName: FqName): Set<Name> =
        KotlinTopLevelClassByPackageIndex.getInstance()
            .get(packageFqName.asStringForIndexes(), project, searchScope)
            .mapNotNullTo(hashSetOf()) { it.nameAsName }


    companion object {
        private fun CallableId.asStringForIndexes(): String =
            (if (packageName.isRoot) callableName.asString() else toString()).replace('/', '.')

        private fun FqName.asStringForIndexes(): String =
            asString().replace('/', '.')

        private fun ClassId.asStringForIndexes(): String =
            asSingleFqName().asStringForIndexes()
    }
}

class KtDeclarationProviderFactoryIdeImpl(private val project: Project) : KtDeclarationProviderFactory() {
    override fun createDeclarationProvider(searchScope: GlobalSearchScope): DeclarationProvider {
        return DeclarationProviderByIndexesImpl(project, searchScope)
    }
}