/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

@KaPlatformInterface
public object KotlinEmptyDeclarationProvider : KotlinDeclarationProvider {
    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? = null
    override fun getAllClassesByClassId(classId: ClassId): List<KtClassOrObject> = []
    override fun getAllTypeAliasesByClassId(classId: ClassId): List<KtTypeAlias> = []
    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> = []
    override fun getTopLevelProperties(callableId: CallableId): List<KtProperty> = []
    override fun getTopLevelFunctions(callableId: CallableId): List<KtNamedFunction> = []
    override fun getTopLevelCallableFiles(callableId: CallableId): List<KtFile> = []
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = []
    override fun findFilesForFacadeByPackage(packageFqName: FqName): List<KtFile> = []
    override fun findFilesForFacade(facadeFqName: FqName): List<KtFile> = []
    override fun findInternalFilesForFacade(facadeFqName: FqName): List<KtFile> = []
    override fun findFilesForScript(scriptFqName: FqName): List<KtScript> = []

    override fun computePackageNames(): Set<String> = []
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
}
