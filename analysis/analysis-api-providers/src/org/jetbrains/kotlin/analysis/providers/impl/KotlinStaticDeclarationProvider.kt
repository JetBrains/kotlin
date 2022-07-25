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

public class KotlinStaticDeclarationIndex {
    internal val facadeFileMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    internal val classMap: MutableMap<FqName, MutableSet<KtClassOrObject>> = mutableMapOf()
    internal val typeAliasMap: MutableMap<FqName, MutableSet<KtTypeAlias>> = mutableMapOf()
    internal val topLevelFunctionMap: MutableMap<FqName, MutableSet<KtNamedFunction>> = mutableMapOf()
    internal val topLevelPropertyMap: MutableMap<FqName, MutableSet<KtProperty>> = mutableMapOf()
}

public class KotlinStaticDeclarationProvider internal constructor(
    private val index: KotlinStaticDeclarationIndex,
    private val scope: GlobalSearchScope,
) : KotlinDeclarationProvider() {

    private val KtElement.inScope: Boolean
        get() = containingKtFile.virtualFile in scope

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        return getAllClassesByClassId(classId).firstOrNull()
            ?: getAllTypeAliasesByClassId(classId).firstOrNull()
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        index.classMap[classId.packageFqName]
            ?.filter { ktClassOrObject ->
                ktClassOrObject.getClassId() == classId && ktClassOrObject.inScope
            }
            ?: emptyList()

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        index.typeAliasMap[classId.packageFqName]
            ?.filter { ktTypeAlias ->
                ktTypeAlias.getClassId() == classId && ktTypeAlias.inScope
            }
            ?: emptyList()


    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        val classifiers = index.classMap[packageFqName].orEmpty() + index.typeAliasMap[packageFqName].orEmpty()
        return classifiers.filter { it.inScope }
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
    }


    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        val callables = index.topLevelPropertyMap[packageFqName].orEmpty() + index.topLevelFunctionMap[packageFqName].orEmpty()
        return callables
            .filter { it.inScope }
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
    }


    override fun getFacadeFilesInPackage(packageFqName: FqName): Collection<KtFile> =
        index.facadeFileMap[packageFqName]
            ?.filter { ktFile ->
                ktFile.virtualFile in scope
            }
            ?: emptyList()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return getFacadeFilesInPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
            .filter { it.javaFileFacadeFqName == facadeFqName }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        index.topLevelPropertyMap[callableId.packageName]
            ?.filter { ktProperty ->
                ktProperty.nameAsName == callableId.callableName && ktProperty.inScope
            }
            ?: emptyList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        index.topLevelFunctionMap[callableId.packageName]
            ?.filter { ktNamedFunction ->
                ktNamedFunction.nameAsName == callableId.callableName && ktNamedFunction.inScope
            }
            ?: emptyList()

}

public class KotlinStaticDeclarationProviderFactory(
    files: Collection<KtFile>
) : KotlinDeclarationProviderFactory() {

    private val index = KotlinStaticDeclarationIndex()

    private class KtDeclarationRecorder(private val index: KotlinStaticDeclarationIndex) : KtVisitorVoid() {

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
        files.forEach {
            it.accept(recorder)
        }
    }

    override fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
        return KotlinStaticDeclarationProvider(index, searchScope)
    }
}
