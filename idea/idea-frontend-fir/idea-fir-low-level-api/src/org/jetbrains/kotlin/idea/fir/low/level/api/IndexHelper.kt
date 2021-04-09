/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

/*
* Move to another module
*/
public class IndexHelper(val project: Project, private val scope: GlobalSearchScope) {
    private val stubIndex: StubIndex = StubIndex.getInstance()

    private inline fun <INDEX_KEY : Any, reified PSI : PsiElement> firstMatchingOrNull(
        stubKey: StubIndexKey<INDEX_KEY, PSI>,
        key: INDEX_KEY,
        crossinline filter: (PSI) -> Boolean = { true }
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

    fun typeAliasFromIndexByClassId(classId: ClassId): KtTypeAlias? = firstMatchingOrNull<String, KtTypeAlias>(
        KotlinTopLevelTypeAliasFqNameIndex.KEY,
        key = classId.asStringForIndexes(),
    ) { candidate -> candidate.containingKtFile.packageFqName == classId.packageFqName }
        ?: firstMatchingOrNull<String, KtTypeAlias>(
            KotlinInnerTypeAliasClassIdIndex.KEY,
            key = classId.asString(),
        )


    fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        KotlinTopLevelPropertyFqnNameIndex.getInstance()[callableId.asStringForIndexes(), project, scope]

    fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        KotlinTopLevelFunctionFqnNameIndex.getInstance()[callableId.asStringForIndexes(), project, scope]

    fun getTopLevelPropertiesInPackage(packageFqName: FqName): Collection<KtProperty> =
        KotlinTopLevelPropertyByPackageIndex.getInstance().get(packageFqName.asStringForIndexes(), project, scope)

    fun getTopLevelFunctionsInPackage(packageFqName: FqName): Collection<KtFunction> =
        KotlinTopLevelFunctionByPackageIndex.getInstance().get(packageFqName.asStringForIndexes(), project, scope)

    fun getClassNamesInPackage(packageFqName: FqName): Set<Name> =
        KotlinTopLevelClassByPackageIndex.getInstance()
            .get(packageFqName.asStringForIndexes(), project, scope)
            .mapNotNullTo(hashSetOf()) { it.nameAsName }

    fun getKotlinClasses(
        nameFilter: (Name) -> Boolean,
        psiFilter: (element: KtClassOrObject) -> Boolean = { true }
    ): Collection<KtClassOrObject> {
        val index = KotlinFullClassNameIndex.getInstance()
        return index.getAllKeys(project).asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .filter { fqName -> nameFilter(getShortName(fqName)) }
            .flatMap { fqName -> index[fqName, project, scope] }
            .filter(psiFilter)
            .toList()
    }

    fun getTopLevelCallables(nameFilter: (Name) -> Boolean): Collection<KtCallableDeclaration> {
        fun sequenceOfElements(index: StringStubIndexExtension<out KtCallableDeclaration>): Sequence<KtCallableDeclaration> =
            index.getAllKeys(project).asSequence()
                .onEach { ProgressManager.checkCanceled() }
                .filter { fqName -> nameFilter(getShortName(fqName)) }
                .flatMap { fqName -> index[fqName, project, scope] }
                .filter { it.receiverTypeReference == null }

        val functions = sequenceOfElements(KotlinTopLevelFunctionFqnNameIndex.getInstance())
        val properties = sequenceOfElements(KotlinTopLevelPropertyFqnNameIndex.getInstance())

        return (functions + properties).toList()
    }

    fun getTopLevelExtensions(nameFilter: (Name) -> Boolean, receiverTypeNames: Set<String>): Collection<KtCallableDeclaration> {
        val index = KotlinTopLevelExtensionsByReceiverTypeIndex.INSTANCE

        return index.getAllKeys(project).asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .filter { KotlinTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames }
            .filter { nameFilter(Name.identifier(KotlinTopLevelExtensionsByReceiverTypeIndex.callableNameFromKey(it))) }
            .flatMap { key -> index[key, project, scope] }
            .toList()
    }

    fun getPossibleTypeAliasExpansionNames(originalTypeName: String): Set<String> {
        val index = KotlinTypeAliasByExpansionShortNameIndex.INSTANCE
        val out = mutableSetOf<String>()

        fun searchRecursively(typeName: String) {
            ProgressManager.checkCanceled()
            index[typeName, project, scope].asSequence()
                .mapNotNull { it.name }
                .filter { out.add(it) }
                .forEach(::searchRecursively)
        }

        searchRecursively(originalTypeName)
        return out
    }

    companion object {
        private fun CallableId.asStringForIndexes(): String =
            (if (packageName.isRoot) callableName.asString() else toString()).replace('/', '.')

        private fun FqName.asStringForIndexes(): String =
            asString().replace('/', '.')

        private fun ClassId.asStringForIndexes(): String =
            asSingleFqName().asStringForIndexes()

        private fun getShortName(fqName: String) = Name.identifier(fqName.substringAfterLast('.'))
    }
}