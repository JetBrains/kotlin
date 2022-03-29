/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public class KotlinStaticDeclarationProvider(
    scope: GlobalSearchScope,
    ktFiles: Collection<KtFile>
) : KotlinDeclarationProvider() {

    private val index = Index()

    private class Index {
        val facadeFileMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
        val classMap: MutableMap<FqName, MutableSet<KtClassOrObject>> = mutableMapOf()
        val typeAliasMap: MutableMap<FqName, MutableSet<KtTypeAlias>> = mutableMapOf()
        val topLevelFunctionMap: MutableMap<FqName, MutableSet<KtNamedFunction>> = mutableMapOf()
        val topLevelPropertyMap: MutableMap<FqName, MutableSet<KtProperty>> = mutableMapOf()
    }

    private class KtDeclarationRecorder(private val index: Index) : KtVisitorVoid() {

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtFile(file: KtFile) {
            if (file.hasTopLevelCallables()) {
                index.facadeFileMap.computeIfAbsent(file.packageFqName) {
                    mutableSetOf()
                }.add(file)
            }
            super.visitKtFile(file)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            classOrObject.getClassId()?.let { classId ->
                index.classMap.computeIfAbsent(classId.packageFqName) {
                    mutableSetOf()
                }.add(classOrObject)
            }
            super.visitClassOrObject(classOrObject)
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            typeAlias.getClassId()?.let { classId ->
                index.typeAliasMap.computeIfAbsent(classId.packageFqName) {
                    mutableSetOf()
                }.add(typeAlias)
            }
            super.visitTypeAlias(typeAlias)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            if (function.isTopLevel) {
                val packageFqName = (function.parent as KtFile).packageFqName
                index.topLevelFunctionMap.computeIfAbsent(packageFqName) {
                    mutableSetOf()
                }.add(function)
            }
            super.visitNamedFunction(function)
        }

        override fun visitProperty(property: KtProperty) {
            if (property.isTopLevel) {
                val packageFqName = (property.parent as KtFile).packageFqName
                index.topLevelPropertyMap.computeIfAbsent(packageFqName) {
                    mutableSetOf()
                }.add(property)
            }
            super.visitProperty(property)
        }
    }

    init {
        val recorder = KtDeclarationRecorder(index)
        ktFiles
            .filter {
                scope.contains(it.virtualFile)
            }
            .forEach {
                it.accept(recorder)
            }
    }

    override fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        index.classMap[classId.packageFqName]
            ?.filter { it.getClassId() == classId }
            ?: emptyList()

    override fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        index.typeAliasMap[classId.packageFqName]
            ?.filter { it.getClassId() == classId }
            ?: emptyList()

    override fun getClassNamesInPackage(packageFqName: FqName): Set<Name> =
        index.classMap[packageFqName]
            ?.mapNotNullTo(mutableSetOf()) { it.nameAsName }
            ?: emptySet()

    override fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name> =
        index.typeAliasMap[packageFqName]
            ?.mapNotNullTo(mutableSetOf()) { it.nameAsName }
            ?: emptySet()

    override fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name> =
        index.topLevelPropertyMap[packageFqName]
            ?.mapNotNullTo(mutableSetOf()) { it.nameAsName }
            ?: emptySet()

    override fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name> =
        index.topLevelFunctionMap[packageFqName]
            ?.mapNotNullTo(mutableSetOf()) { it.nameAsName }
            ?: emptySet()

    override fun getFacadeFilesInPackage(packageFqName: FqName): Collection<KtFile> =
        index.facadeFileMap[packageFqName]
            ?: emptyList()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return getFacadeFilesInPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
            .filter { it.javaFileFacadeFqName == facadeFqName }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        index.topLevelPropertyMap[callableId.packageName]
            ?.filter { it.nameAsName == callableId.callableName }
            ?: emptyList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        index.topLevelFunctionMap[callableId.packageName]
            ?.filter { it.nameAsName == callableId.callableName }
            ?: emptyList()

}

public class KotlinStaticDeclarationProviderFactory(private val files: Collection<KtFile>) : KotlinDeclarationProviderFactory() {
    override fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
        return KotlinStaticDeclarationProvider(searchScope, files)
    }
}
