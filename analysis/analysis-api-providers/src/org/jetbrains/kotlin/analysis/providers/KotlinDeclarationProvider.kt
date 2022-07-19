/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * A declaration provider for a given scope. Can be created via [KotlinDeclarationProviderFactory].
 * May be called frequently, so for implementations it is better to cache results.
 */
public abstract class KotlinDeclarationProvider {
    public abstract fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration?

    public abstract fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject>
    public abstract fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias>

    public abstract fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name>

    public abstract fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>
    public abstract fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>

    public abstract fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile>

    public abstract fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>

    public abstract fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile>
    public abstract fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile>

    /**
     * Currently we want only classes from libraries ([org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile])
     */
    public abstract fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile>
}

public abstract class KotlinDeclarationProviderFactory {
    public abstract fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider
}

public fun Project.createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider =
    this.getService(KotlinDeclarationProviderFactory::class.java)
        .createDeclarationProvider(searchScope)