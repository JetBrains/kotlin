/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

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

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> =
        index.facadeFileMap[packageFqName]
            ?.filter { ktFile ->
                ktFile.virtualFile in scope
            }
            ?: emptyList()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return findFilesForFacadeByPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
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

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> = buildSet {
        getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
        getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
    }

}

public class KotlinStaticDeclarationProviderFactory(
    private val project: Project,
    files: Collection<KtFile>,
    private val jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
) : KotlinDeclarationProviderFactory() {

    private val index = KotlinStaticDeclarationIndex()

    private val psiManager = PsiManager.getInstance(project)
    private val builtInDecompiler = KotlinBuiltInDecompiler()

    private fun loadBuiltIns(): Collection<KtDecompiledFile> {
        val classLoader = this::class.java.classLoader
        return buildList {
            StandardClassIds.builtInsPackages.forEach { builtInPackageFqName ->
                val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)
                classLoader.getResource(resourcePath)?.let { resourceUrl ->
                    // "file:///path/to/stdlib.jar!/builtin/package/.kotlin_builtins
                    //   -> ("path/to/stdlib.jar", "builtin/package/.kotlin_builtins")
                    URLUtil.splitJarUrl(resourceUrl.path)?.let {
                        val jarPath = it.first
                        val builtInFile = it.second
                        val pathToQuery = jarPath + URLUtil.JAR_SEPARATOR + builtInFile
                        jarFileSystem.findFileByPath(pathToQuery)?.let { vf ->
                            val fileContent = FileContentImpl.createByFile(vf, project)
                            createKtFileStub(fileContent)?.let { file -> add(file) }
                        }
                    }
                }
            }
        }
    }

    private fun createKtFileStub(
        fileContent: FileContent,
    ): KtDecompiledFile? {
        val fileViewProvider =
            builtInDecompiler.createFileViewProvider(fileContent.file, psiManager, physical = true) as? KotlinDecompiledFileViewProvider
                ?: return null
        return builtInDecompiler.readFile(fileContent.content, fileContent.file)?.let { fileWithMetadata ->
            KtDecompiledFile(fileViewProvider) {
                builtInDecompiler.buildDecompiledText(fileWithMetadata)
            }
        }
    }

    private inner class KtDeclarationRecorder : KtVisitorVoid() {

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtFile(file: KtFile) {
            addToFacadeFileMap(file)
            super.visitKtFile(file)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            addToClassMap(classOrObject)
            super.visitClassOrObject(classOrObject)
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            addToTypeAliasMap(typeAlias)
            super.visitTypeAlias(typeAlias)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            addToFunctionMap(function)
            super.visitNamedFunction(function)
        }

        override fun visitProperty(property: KtProperty) {
            addToPropertyMap(property)
            super.visitProperty(property)
        }
    }

    private fun addToFacadeFileMap(file: KtFile) {
        if (!file.hasTopLevelCallables()) return
        index.facadeFileMap.computeIfAbsent(file.packageFqName) {
            mutableSetOf()
        }.add(file)
    }

    private fun addToClassMap(classOrObject: KtClassOrObject) {
        classOrObject.getClassId()?.let { classId ->
            index.classMap.computeIfAbsent(classId.packageFqName) {
                mutableSetOf()
            }.add(classOrObject)
        }
    }

    private fun addToTypeAliasMap(typeAlias: KtTypeAlias) {
        typeAlias.getClassId()?.let { classId ->
            index.typeAliasMap.computeIfAbsent(classId.packageFqName) {
                mutableSetOf()
            }.add(typeAlias)
        }
    }

    private fun addToFunctionMap(function: KtNamedFunction) {
        if (!function.isTopLevel) return
        val packageFqName = (function.parent as KtFile).packageFqName
        index.topLevelFunctionMap.computeIfAbsent(packageFqName) {
            mutableSetOf()
        }.add(function)
    }

    private fun addToPropertyMap(property: KtProperty) {
        if (!property.isTopLevel) return
        val packageFqName = (property.parent as KtFile).packageFqName
        index.topLevelPropertyMap.computeIfAbsent(packageFqName) {
            mutableSetOf()
        }.add(property)
    }

    init {
        val recorder = KtDeclarationRecorder()

        // Indexing built-ins
        loadBuiltIns().forEach {
            it.accept(recorder)
        }

        // Indexing user source files
        files.forEach {
            it.accept(recorder)
        }
    }

    override fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
        return KotlinStaticDeclarationProvider(index, searchScope)
    }
}
