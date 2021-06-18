/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.test.base

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.fir.low.level.api.DeclarationProvider
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType

internal class DeclarationProviderTestImpl(
    searchScope: GlobalSearchScope,
    ktFiles: Collection<KtFile>
) : DeclarationProvider() {

    private val filesInScope = ktFiles.filter { searchScope.contains(it.virtualFile) }

    private fun filesByPackage(packageFqName: FqName) =
        filesInScope.asSequence()
            .filter { it.packageFqName == packageFqName }


    override fun getClassByClassId(classId: ClassId): KtClassOrObject? =
        filesByPackage(classId.packageFqName).firstNotNullOfOrNull { file ->
            file.findDescendantOfType { ktClass ->
                ktClass.getClassId() == classId
            }
        }


    override fun getTypeAliasByClassId(classId: ClassId): KtTypeAlias? =
        filesByPackage(classId.packageFqName).firstNotNullOfOrNull { file ->
            file.findDescendantOfType { typeAlias ->
                typeAlias.getClassId() == classId
            }
        }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        filesByPackage(callableId.packageName)
            .flatMap { it.declarations }
            .filterIsInstance<KtProperty>()
            .filter { it.nameAsName == callableId.callableName }
            .toList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        filesByPackage(callableId.packageName)
            .flatMap { it.declarations }
            .filterIsInstance<KtNamedFunction>()
            .filter { it.nameAsName == callableId.callableName }
            .toList()


    override fun getClassNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtClassOrObject>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
}