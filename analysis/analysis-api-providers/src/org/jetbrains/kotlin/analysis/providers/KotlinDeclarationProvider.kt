/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.components.ServiceManager
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
    public abstract fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject>
    public abstract fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias>

    public abstract fun getClassNamesInPackage(packageFqName: FqName): Set<Name>
    public abstract fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name>

    public abstract fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>
    public abstract fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>

    public abstract fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name>
    public abstract fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name>
    public abstract fun getFacadeFilesInPackage(packageFqName: FqName): Collection<KtFile>
    public abstract fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile>
}

public abstract class KotlinDeclarationProviderFactory {
    public abstract fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider
}

public fun Project.createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider =
    ServiceManager.getService(this, KotlinDeclarationProviderFactory::class.java)
        .createDeclarationProvider(searchScope)