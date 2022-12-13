/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*

internal object EmptyKotlinDeclarationProvider : KotlinDeclarationProvider() {
    override fun getClassLikeDeclarationByClassId(classId: ClassId) = null
    override fun getAllClassesByClassId(classId: ClassId) = emptyList<KtClassOrObject>()
    override fun getAllTypeAliasesByClassId(classId: ClassId) = emptyList<KtTypeAlias>()
    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName) = emptySet<Name>()
    override fun getTopLevelProperties(callableId: CallableId) = emptyList<KtProperty>()
    override fun getTopLevelFunctions(callableId: CallableId) = emptyList<KtNamedFunction>()
    override fun getTopLevelCallableFiles(callableId: CallableId) = emptyList<KtFile>()
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName) = emptySet<Name>()
    override fun findFilesForFacadeByPackage(packageFqName: FqName) = emptyList<KtFile>()
    override fun findFilesForFacade(facadeFqName: FqName) = emptyList<KtFile>()
}