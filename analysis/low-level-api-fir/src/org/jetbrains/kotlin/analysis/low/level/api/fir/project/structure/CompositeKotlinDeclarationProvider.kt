/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

class CompositeKotlinDeclarationProvider
private constructor(
    private val providers: List<KotlinDeclarationProvider>
) : KotlinDeclarationProvider() {

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        return providers.firstNotNullOfOrNull { it.getClassLikeDeclarationByClassId(classId) }
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        return providers.flatMap { it.getAllClassesByClassId(classId) }
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        return providers.flatMap { it.getAllTypeAliasesByClassId(classId) }
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName) }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelProperties(callableId) }
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelFunctions(callableId) }
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        return providers.flatMapTo(mutableListOf()) { it.getTopLevelCallableFiles(callableId) }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return providers.flatMapTo(mutableSetOf()) { it.getTopLevelCallableNamesInPackage(packageFqName) }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForFacadeByPackage(packageFqName) }
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForFacade(facadeFqName) }
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return providers.flatMapTo(mutableListOf()) { it.findInternalFilesForFacade(facadeFqName) }
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        return providers.flatMapTo(mutableListOf()) { it.findFilesForScript(scriptFqName) }
    }

     override fun computePackageSetWithTopLevelCallableDeclarations(): Set<String> {
        return providers.flatMapTo(mutableSetOf()) { it.computePackageSetWithTopLevelCallableDeclarations() }
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
