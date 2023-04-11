/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectForEach
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

internal class CompositeKotlinDeclarationProvider
private constructor(
    private val providers: List<KotlinDeclarationProvider>
) : KotlinDeclarationProvider() {

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        return providers.firstNotNullOfOrNull { it.getClassLikeDeclarationByClassId(classId) }
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        collectForEach(mutableListOf()) { addAll(it.getAllClassesByClassId(classId)) }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        return collectForEach(mutableListOf()) { addAll(it.getAllTypeAliasesByClassId(classId)) }
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return collectForEach(mutableSetOf()) { addAll(it.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)) }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        return collectForEach(mutableListOf()) { addAll(it.getTopLevelProperties(callableId)) }
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        return collectForEach(mutableListOf()) { addAll(it.getTopLevelFunctions(callableId)) }
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        return collectForEach(mutableListOf()) { addAll(it.getTopLevelCallableFiles(callableId)) }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return collectForEach(mutableSetOf()) { addAll(it.getTopLevelCallableNamesInPackage(packageFqName)) }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        return collectForEach(mutableListOf()) { addAll(it.findFilesForFacadeByPackage(packageFqName)) }
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return collectForEach(mutableListOf()) { addAll(it.findFilesForFacade(facadeFqName)) }
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return collectForEach(mutableListOf()) { addAll(it.findInternalFilesForFacade(facadeFqName)) }
    }


    private inline fun <E, C : MutableCollection<E>> collectForEach(
        collection: C,
        collect: C.(KotlinDeclarationProvider) -> Unit,
    ): C {
        return providers.collectForEach(collection, collect)
    }

    companion object {
        fun create(providers: List<KotlinDeclarationProvider>): KotlinDeclarationProvider {
            return when (providers.size) {
                0 -> EmptyKotlinDeclarationProvider
                1 -> providers.single()
                else -> CompositeKotlinDeclarationProvider(providers)
            }
        }
    }
}
