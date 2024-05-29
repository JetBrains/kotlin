/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl.declarationProviders

import org.jetbrains.kotlin.analysis.providers.KotlinCompositeProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.impl.KotlinCompositeProviderFactory
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.flatMapToNullableSet

public class CompositeKotlinDeclarationProvider private constructor(
    override val providers: List<KotlinDeclarationProvider>
) : KotlinDeclarationProvider(), KotlinCompositeProvider<KotlinDeclarationProvider> {
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

    override fun computePackageNames(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNames() }
    }

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = providers.any { it.hasSpecificClassifierPackageNamesComputation }

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNamesWithTopLevelClassifiers() }
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = providers.any { it.hasSpecificCallablePackageNamesComputation }

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? {
        return providers.flatMapToNullableSet { it.computePackageNamesWithTopLevelCallables() }
    }

    public companion object {
        public val factory: KotlinCompositeProviderFactory<KotlinDeclarationProvider> = KotlinCompositeProviderFactory(
            EmptyKotlinDeclarationProvider,
            ::CompositeKotlinDeclarationProvider,
        )

        public fun create(providers: List<KotlinDeclarationProvider>): KotlinDeclarationProvider = factory.create(providers)
    }
}
