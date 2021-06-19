/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias

abstract class DeclarationProvider {
    abstract val searchScope: GlobalSearchScope

    abstract fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject>
    abstract fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias>

    abstract fun getClassNamesInPackage(packageFqName: FqName): Set<Name>
    abstract fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name>

    abstract fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>
    abstract fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>

    abstract fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name>
    abstract fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name>
}

abstract class KtDeclarationProviderFactory {
    abstract fun createDeclarationProvider(searchScope: GlobalSearchScope): DeclarationProvider
}

fun Project.createDeclarationProvider(searchScope: GlobalSearchScope): DeclarationProvider =
    ServiceManager.getService(this, KtDeclarationProviderFactory::class.java)
        .createDeclarationProvider(searchScope)