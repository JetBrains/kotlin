/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubElement
import com.intellij.util.containers.CollectionFactory.createConcurrentWeakValueMap
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.URLUtil
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.impl.util.mergeOnly
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

public class KotlinStaticDeclarationProvider internal constructor(
    private val index: KotlinStaticDeclarationIndex,
    public val scope: GlobalSearchScope,
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

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        return index.facadeFileMap[packageFqName].orEmpty().filter { it.virtualFile in scope }
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return findFilesForFacadeByPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
            .filter { it.javaFileFacadeFqName == facadeFqName }
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return index.multiFileClassPartMap[facadeFqName].orEmpty().filter { it.virtualFile in scope }
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        return index.scriptMap[scriptFqName].orEmpty().filter { it.containingKtFile.virtualFile in scope }
    }

    override fun computePackageSetWithTopLevelCallableDeclarations(): Set<String> {
        val packageNames = index.topLevelPropertyMap.keys + index.topLevelFunctionMap.keys
        return packageNames.mapTo(mutableSetOf()) { it.asString() }
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
    additionalRoots: List<VirtualFile> = emptyList(),
) : KotlinDeclarationProviderFactory() {

    private val index = KotlinStaticDeclarationIndex()

    private val psiManager = PsiManager.getInstance(project)
    private val builtInDecompiler = KotlinBuiltInDecompiler()

    private fun loadBuiltIns(): Collection<KotlinFileStubImpl> {
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
                            createKtFileStub(psiManager, builtInDecompiler, fileContent)?.let { file -> add(file) }
                        }
                    }
                }
            }
        }
    }

    private fun createKtFileStub(
        psiManager: PsiManager,
        builtInDecompiler: KotlinBuiltInDecompiler,
        fileContent: FileContent,
    ): KotlinFileStubImpl? {
        val ktFileStub = builtInDecompiler.stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl ?: return null
        val fakeFile = object : KtFile(KtClassFileViewProvider(psiManager, fileContent.file), isCompiled = true) {
            override fun getStub() = ktFileStub

            override fun isPhysical() = false
        }
        ktFileStub.psi = fakeFile
        return ktFileStub
    }

    private class KtClassFileViewProvider(
        psiManager: PsiManager,
        virtualFile: VirtualFile,
    ) : SingleRootFileViewProvider(psiManager, virtualFile, true, KotlinLanguage.INSTANCE)

    private inner class KtDeclarationRecorder : KtVisitorVoid() {

        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtFile(file: KtFile) {
            addToFacadeFileMap(file)
            file.script?.let { addToScriptMap(it) }
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

    private fun addToScriptMap(script: KtScript) {
        index.scriptMap.computeIfAbsent(script.fqName) {
            mutableSetOf()
        }.add(script)
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
        fun indexStub(stub: StubElement<*>) {
            when (stub) {
                is KotlinClassStubImpl -> {
                    addToClassMap(stub.psi)
                    // member functions and properties
                    stub.childrenStubs.forEach(::indexStub)
                }
                is KotlinObjectStubImpl -> {
                    addToClassMap(stub.psi)
                    // member functions and properties
                    stub.childrenStubs.forEach(::indexStub)
                }
                is KotlinTypeAliasStubImpl -> addToTypeAliasMap(stub.psi)
                is KotlinFunctionStubImpl -> addToFunctionMap(stub.psi)
                is KotlinPropertyStubImpl -> addToPropertyMap(stub.psi)
                is KotlinPlaceHolderStubImpl -> {
                    if (stub.stubType == KtStubElementTypes.CLASS_BODY) {
                        stub.getChildrenStubs().filterIsInstance<KotlinClassOrObjectStub<*>>().forEach(::indexStub)
                    }
                }
            }
        }

        fun processStub(ktFileStub: KotlinFileStubImpl) {
            val ktFile: KtFile = ktFileStub.psi
            addToFacadeFileMap(ktFile)

            val partNames = ktFileStub.facadePartSimpleNames
            if (partNames != null) {
                val packageFqName = ktFileStub.getPackageFqName()
                for (partName in partNames) {
                    val multiFileClassPartFqName: FqName = packageFqName.child(Name.identifier(partName))
                    index.multiFileClassPartMap.computeIfAbsent(multiFileClassPartFqName) { mutableSetOf() }.add(ktFile)
                }
            }

            // top-level functions and properties, built-in classes
            ktFileStub.childrenStubs.forEach(::indexStub)
        }

        val builtins = mutableSetOf<String>()
        loadBuiltIns().forEach { stub ->
            processStub(stub)
            builtins.add(stub.psi.virtualFile.name)
        }

        val binaryClassCache = ClsKotlinBinaryClassCache.getInstance()
        for (root in additionalRoots) {
            KotlinFakeClsStubsCache.processAdditionalRoot(root) { additionalRoot ->
                val stubs = hashMapOf<VirtualFile, KotlinFileStubImpl>()
                VfsUtilCore.visitChildrenRecursively(additionalRoot, object : VirtualFileVisitor<Void>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        if (!file.isDirectory) {
                            val fileContent = FileContentImpl.createByFile(file)
                            if (binaryClassCache.isKotlinJvmCompiledFile(file, fileContent.content) &&
                                file.fileType == JavaClassFileType.INSTANCE
                            ) {
                                (KotlinClsStubBuilder().buildFileStub(fileContent) as? KotlinFileStubImpl)?.let { stubs.put(file, it) }
                            }
                        }
                        return true
                    }
                })
                stubs
            }?.forEach { entry ->
                val stub = entry.value
                val fakeFile = object : KtFile(KtClassFileViewProvider(psiManager, entry.key), isCompiled = true) {
                    override fun getStub() = stub
                    override fun isPhysical() = false
                }
                stub.psi = fakeFile
                processStub(stub)
            }
        }

        // Indexing user source files
        files.forEach {
            it.accept(recorder)
        }
    }

    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KtModule?): KotlinDeclarationProvider {
        return KotlinStaticDeclarationProvider(index, scope)
    }
}

/**
 * Test application service to store stubs of shared between tests libraries.
 *
 * Otherwise, each test would start indexing of stdlib from scratch,
 * and under the lock which makes tests extremely slow*/
public class KotlinFakeClsStubsCache {
    private val fakeFileClsStubs = ConcurrentHashMap<String, Map<VirtualFile, KotlinFileStubImpl>>()

    public companion object {
        public fun processAdditionalRoot(
            root: VirtualFile,
            storage: (VirtualFile) -> Map<VirtualFile, KotlinFileStubImpl>
        ): Map<VirtualFile, KotlinFileStubImpl>? {
            val service = ApplicationManager.getApplication().getService(KotlinFakeClsStubsCache::class.java) ?: return null
            if (service.fakeFileClsStubs[root.path] == null) {
                service.fakeFileClsStubs[root.path] = storage(root)
            }
            return service.fakeFileClsStubs.computeIfAbsent(root.path) { _ ->
                storage(root)
            }
        }
    }
}

public class KotlinStaticDeclarationProviderMerger(private val project: Project) : KotlinDeclarationProviderMergerBase() {
    override fun mergeToList(declarationProviders: List<KotlinDeclarationProvider>): List<KotlinDeclarationProvider> =
        declarationProviders.mergeOnly<_, KotlinStaticDeclarationProvider> { providers ->
            val combinedScope = GlobalSearchScope.union(providers.map { it.scope })
            project.createDeclarationProvider(combinedScope, module = null).apply {
                check(this is KotlinStaticDeclarationProvider) {
                    "`KotlinStaticDeclarationProvider` can only be merged into a combined declaration provider of the same type."
                }
            }
        }
}
