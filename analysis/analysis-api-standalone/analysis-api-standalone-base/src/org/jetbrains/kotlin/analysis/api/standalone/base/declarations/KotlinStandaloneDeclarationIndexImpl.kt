/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getImportedSimpleNameByImportAlias
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.utils.addIfNotNull

internal class KotlinStandaloneDeclarationIndexImpl : KotlinStandaloneDeclarationIndex {
    override val facadeFileMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    override val multiFileClassPartMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    override val scriptMap: MutableMap<FqName, MutableSet<KtScript>> = mutableMapOf()

    override val classesByClassId: MutableMap<ClassId, MutableSet<KtClassOrObject>> = mutableMapOf()
    override val typeAliasesByClassId: MutableMap<ClassId, MutableSet<KtTypeAlias>> = mutableMapOf()
    override val topLevelFunctionsByCallableId: MutableMap<CallableId, MutableSet<KtNamedFunction>> = mutableMapOf()
    override val topLevelPropertiesByCallableId: MutableMap<CallableId, MutableSet<KtProperty>> = mutableMapOf()

    override val classLikeDeclarationsByPackage: MutableMap<FqName, MutableSet<KtClassLikeDeclaration>> = mutableMapOf()
    override val topLevelCallablesByPackage: MutableMap<FqName, MutableSet<KtCallableDeclaration>> = mutableMapOf()

    override val classesBySupertypeName: MutableMap<Name, MutableSet<KtClassOrObject>> = mutableMapOf()
    override val inheritableTypeAliasesByAliasedName: MutableMap<Name, MutableSet<KtTypeAlias>> = mutableMapOf()

    private fun indexFile(file: KtFile) {
        if (!file.hasTopLevelCallables()) return
        facadeFileMap.computeIfAbsent(file.packageFqName) {
            mutableSetOf()
        }.add(file)
    }

    private fun indexScript(script: KtScript) {
        scriptMap.computeIfAbsent(script.fqName) {
            mutableSetOf()
        }.add(script)
    }

    private fun indexClassOrObject(classOrObject: KtClassOrObject) {
        indexClassLikeDeclaration(classOrObject, classesByClassId)

        classOrObject.getSuperNames().forEach { superName ->
            classesBySupertypeName
                .computeIfAbsent(Name.identifier(superName)) { mutableSetOf() }
                .add(classOrObject)
        }
    }

    private fun indexTypeAlias(typeAlias: KtTypeAlias) {
        indexClassLikeDeclaration(typeAlias, typeAliasesByClassId)

        typeAlias.getTypeReference()?.typeElement?.let { typeElement ->
            findInheritableSimpleNames(typeElement).forEach { expandedName ->
                inheritableTypeAliasesByAliasedName
                    .computeIfAbsent(Name.identifier(expandedName)) { mutableSetOf() }
                    .add(typeAlias)
            }
        }
    }

    private fun <T : KtClassLikeDeclaration> indexClassLikeDeclaration(
        classLikeDeclaration: T,
        classLikeDeclarationsByClassId: MutableMap<ClassId, MutableSet<T>>,
    ) {
        val classId = classLikeDeclaration.getClassId() ?: return

        classLikeDeclarationsByClassId.computeIfAbsent(classId) {
            mutableSetOf()
        }.add(classLikeDeclaration)

        classLikeDeclarationsByPackage.computeIfAbsent(classId.packageFqName) {
            mutableSetOf()
        }.add(classLikeDeclaration)
    }

    private fun processMultifileClassStub(compiledStub: KotlinFileStubImpl, decompiledFile: KtFile) {
        val partNames = compiledStub.facadePartSimpleNames ?: return
        val packageFqName = compiledStub.getPackageFqName()
        for (partName in partNames) {
            val multiFileClassPartFqName: FqName = packageFqName.child(Name.identifier(partName))
            multiFileClassPartMap.computeIfAbsent(multiFileClassPartFqName) { mutableSetOf() }.add(decompiledFile)
        }
    }

    private fun indexNamedFunction(function: KtNamedFunction) {
        if (!function.isTopLevel) return

        indexTopLevelCallable(function, topLevelFunctionsByCallableId)
    }

    private fun indexProperty(property: KtProperty) {
        if (!property.isTopLevel) return

        indexTopLevelCallable(property, topLevelPropertiesByCallableId)
    }

    private fun <T : KtCallableDeclaration> indexTopLevelCallable(
        callable: T,
        callablesByCallableId: MutableMap<CallableId, MutableSet<T>>,
    ) {
        val name = callable.nameAsName ?: return

        val packageFqName = callable.containingKtFile.packageFqName
        val callableId = CallableId(packageFqName, name)

        callablesByCallableId.computeIfAbsent(callableId) {
            mutableSetOf()
        }.add(callable)

        topLevelCallablesByPackage.computeIfAbsent(packageFqName) {
            mutableSetOf()
        }.add(callable)
    }

    fun indexStubRecursively(stub: StubElement<*>): Unit = when (stub) {
        is KotlinFileStubImpl -> {
            val file = stub.psi
            indexFile(file)
            processMultifileClassStub(stub, file)

            // top-level declarations
            stub.childrenStubs.forEach(::indexStubRecursively)
        }

        is KotlinClassStubImpl -> {
            indexClassOrObject(stub.psi)

            // member declarations
            stub.childrenStubs.forEach(::indexStubRecursively)
        }

        is KotlinObjectStubImpl -> {
            indexClassOrObject(stub.psi)

            // member declarations
            stub.childrenStubs.forEach(::indexStubRecursively)
        }

        is KotlinScriptStubImpl -> {
            indexScript(stub.psi)

            // top-level declarations
            stub.childrenStubs.forEach(::indexStubRecursively)
        }

        is KotlinTypeAliasStubImpl -> indexTypeAlias(stub.psi)
        is KotlinFunctionStubImpl -> indexNamedFunction(stub.psi)
        is KotlinPropertyStubImpl -> indexProperty(stub.psi)
        is KotlinPlaceHolderStubImpl if (@Suppress("DEPRECATION") stub.stubType == KtStubElementTypes.CLASS_BODY) -> {
            stub.childrenStubs
                .filterIsInstance<KotlinClassOrObjectStub<*>>()
                .forEach(::indexStubRecursively)
        }

        else -> Unit
    }

    inner class AstBasedIndexer : KtVisitorVoid() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtFile(file: KtFile) {
            indexFile(file)
            super.visitKtFile(file)
        }

        override fun visitScript(script: KtScript) {
            indexScript(script)
            super.visitScript(script)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            indexClassOrObject(classOrObject)
            super.visitClassOrObject(classOrObject)
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            indexTypeAlias(typeAlias)
            super.visitTypeAlias(typeAlias)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            indexNamedFunction(function)
            super.visitNamedFunction(function)
        }

        override fun visitProperty(property: KtProperty) {
            indexProperty(property)
            super.visitProperty(property)
        }
    }
}

/**
 * This is a simplified version of `KtTypeElement.index()` from the IDE. If we need to move more indexing code to Standalone, we should
 * consider moving more code from the IDE to the Analysis API.
 *
 * @see KotlinStandaloneDeclarationIndex.inheritableTypeAliasesByAliasedName
 */
private fun findInheritableSimpleNames(typeElement: KtTypeElement): List<String> {
    return when (typeElement) {
        is KtUserType -> {
            val referenceName = typeElement.referencedName ?: return emptyList()

            buildList {
                add(referenceName)

                val ktFile = typeElement.containingKtFile
                if (!ktFile.isCompiled) {
                    addIfNotNull(getImportedSimpleNameByImportAlias(typeElement.containingKtFile, referenceName))
                }
            }
        }

        // `typealias T = A?` is inheritable.
        is KtNullableType -> typeElement.innerType?.let(::findInheritableSimpleNames) ?: emptyList()

        else -> emptyList()
    }
}