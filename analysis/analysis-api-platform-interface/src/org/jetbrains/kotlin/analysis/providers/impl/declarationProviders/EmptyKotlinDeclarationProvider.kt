/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl.declarationProviders

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public object EmptyKotlinDeclarationProvider : KotlinDeclarationProvider() {
    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? = null
    override fun getAllClassesByClassId(classId: ClassId): List<KtClassOrObject> = emptyList()
    override fun getAllTypeAliasesByClassId(classId: ClassId): List<KtTypeAlias> = emptyList()
    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
    override fun getTopLevelProperties(callableId: CallableId): List<KtProperty> = emptyList()
    override fun getTopLevelFunctions(callableId: CallableId): List<KtNamedFunction> = emptyList()
    override fun getTopLevelCallableFiles(callableId: CallableId): List<KtFile> = emptyList()
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()
    override fun findFilesForFacadeByPackage(packageFqName: FqName): List<KtFile> = emptyList()
    override fun findFilesForFacade(facadeFqName: FqName): List<KtFile> = emptyList()
    override fun findInternalFilesForFacade(facadeFqName: FqName): List<KtFile> = emptyList()
    override fun findFilesForScript(scriptFqName: FqName): List<KtScript> = emptyList()

    override fun computePackageNames(): Set<String> = emptySet()
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
}
