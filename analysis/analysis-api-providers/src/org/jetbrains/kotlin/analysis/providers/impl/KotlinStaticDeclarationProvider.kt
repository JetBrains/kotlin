/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

public class KotlinStaticDeclarationProvider(
    scope: GlobalSearchScope,
    ktFiles: Collection<KtFile>
) : KotlinDeclarationProvider() {
    private val filesInScope = ktFiles.filter { scope.contains(it.virtualFile) }

    private fun filesByPackage(packageFqName: FqName) =
        filesInScope.asSequence()
            .filter { it.packageFqName == packageFqName }

    override fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        filesByPackage(classId.packageFqName).flatMap { file ->
            file.collectDescendantsOfType<KtClassOrObject> { ktClass ->
                ktClass.getClassId() == classId
            }
        }.toList()

    override fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        filesByPackage(classId.packageFqName).flatMap { file ->
            file.collectDescendantsOfType<KtTypeAlias> { typeAlias ->
                typeAlias.getClassId() == classId
            }
        }.toList()

    override fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtTypeAlias>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtProperty>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtNamedFunction>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getFacadeFilesInPackage(packageFqName: FqName): Collection<KtFile> =
        filesByPackage(packageFqName)
            .filter { file -> file.hasTopLevelCallables() }
            .toSet()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return getFacadeFilesInPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
            .filter { it.javaFileFacadeFqName == facadeFqName }
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

public class KotlinStaticDeclarationProviderFactory(private val files: Collection<KtFile>) : KotlinDeclarationProviderFactory() {
    override fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
        return KotlinStaticDeclarationProvider(searchScope, files)
    }
}