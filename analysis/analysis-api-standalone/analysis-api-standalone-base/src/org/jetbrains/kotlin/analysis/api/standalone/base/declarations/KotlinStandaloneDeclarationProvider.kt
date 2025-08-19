/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.StringRef
import com.intellij.util.io.UnsyncByteArrayOutputStream
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.SmartPointerIncompatiblePsiFile
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.decompiler.konan.K2KotlinNativeMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getImportedSimpleNameByImportAlias
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.util.concurrent.ConcurrentHashMap

class KotlinStandaloneDeclarationProvider internal constructor(
    private val index: KotlinStandaloneDeclarationIndex,
    val scope: GlobalSearchScope,
    private val contextualModule: KaModule?,
    private val environment: CoreApplicationEnvironment,
    private val shouldComputeBinaryLibraryPackageSets: Boolean,
) : KotlinDeclarationProvider {
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

    // It is generally an *advantage* to have non-specific package set computations, as some components only work with general package sets.
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false

    override fun computePackageNames(): Set<String>? =
        when (contextualModule) {
            is KaSourceModule, is KaScriptModule, is KaNotUnderContentRootModule ->
                computePackageSetFromIndex()

            is KaLibraryModule ->
                if (contextualModule.canComputePackageSetFromIndex) {
                    computePackageSetFromIndex()
                } else {
                    computeBinaryLibraryModulePackageSet(contextualModule)
                }

            else -> null
        }

    /**
     * For library modules, we can only compute a package set from the index when we use stubs, as we don't index binary dependencies
     * otherwise.
     */
    private val KaLibraryModule.canComputePackageSetFromIndex: Boolean
        get() = KotlinPlatformSettings.getInstance(project).deserializedDeclarationsOrigin == KotlinDeserializedDeclarationsOrigin.STUBS

    private fun computePackageSetFromIndex(): Set<String> = buildSet {
        addPackageNamesInScope(index.classMap)
        addPackageNamesInScope(index.typeAliasMap)
        addPackageNamesInScope(index.topLevelPropertyMap)
        addPackageNamesInScope(index.topLevelFunctionMap)
    }

    private fun <T : KtDeclaration> MutableSet<String>.addPackageNamesInScope(map: Map<FqName, Set<T>>) {
        map.forEach { (fqName, declarations) ->
            if (declarations.any { it.inScope }) {
                add(fqName.asString())
            }
        }
    }

    /**
     * The computation only supports JARs for now and is intended for test purposes.
     */
    private fun computeBinaryLibraryModulePackageSet(module: KaLibraryModule): Set<String>? {
        if (!shouldComputeBinaryLibraryPackageSets) return null

        // The current situation is a bit awkward in Standalone because we have binary root paths and separate binary virtual files, while
        // the IDE keeps them in sync. See KT-72676 for further information.
        val binaryVirtualFiles =
            StandaloneProjectFactory.getVirtualFilesForLibraryRoots(module.binaryRoots, environment) + module.binaryVirtualFiles

        if (binaryVirtualFiles.any { it.fileSystem != environment.jarFileSystem }) {
            return null
        }

        return buildSet {
            binaryVirtualFiles.forEach { jarRoot ->
                VfsUtilCore.visitChildrenRecursively(jarRoot, object : VirtualFileVisitor<Void>() {
                    override fun visitFileEx(file: VirtualFile): Result {
                        if (file.isDirectory) return CONTINUE

                        if (
                            file.extension == JavaClassFileType.DEFAULT_EXTENSION ||
                            file.fileType == JavaClassFileType.INSTANCE
                        ) {
                            addIfNotNull(reconstructPackageNameForJarClassFile(file, jarRoot))
                        }
                        return CONTINUE
                    }
                })
            }
        }
    }

    /**
     * The function assumes that the directory story of the JAR corresponds to each class's package name (which should be true). This allows
     * us to avoid reading the class file.
     */
    private fun reconstructPackageNameForJarClassFile(virtualFile: VirtualFile, jarRoot: VirtualFile): String? {
        val relativePath = VfsUtilCore.findRelativePath(jarRoot, virtualFile.parent, '/') ?: return null
        return relativePath.trim('.').replace('/', '.')
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

/**
 * [binaryRoots] and [sharedBinaryRoots] are used to build stubs for symbols from binary libraries. They only need to be specified if
 * [shouldBuildStubsForBinaryLibraries] is true. In Standalone mode, binary roots don't need to be specified because library symbols are
 * provided via class-based deserialization, not stub-based deserialization.
 *
 * @param binaryRoots Binary roots of the binary libraries that are specific to [project].
 * @param sharedBinaryRoots Binary roots that are shared between multiple different projects. This allows Kotlin tests to cache stubs for
 *  shared libraries like the Kotlin stdlib.
 * @param shouldComputeBinaryLibraryPackageSets Whether to compute package sets for binary libraries when they are NOT indexed by default.
 *  It is risky to enable this in production because in some file systems, file traversal can be slow. So we shouldn't enable this without
 *  further investigation.
 */
class KotlinStandaloneDeclarationProviderFactory(
    private val project: Project,
    private val environment: CoreApplicationEnvironment,
    sourceKtFiles: Collection<KtFile>,
    binaryRoots: List<VirtualFile> = emptyList(),
    sharedBinaryRoots: List<VirtualFile> = emptyList(),
    skipBuiltins: Boolean = false,
    shouldBuildStubsForBinaryLibraries: Boolean = false,
    private val shouldComputeBinaryLibraryPackageSets: Boolean = false,
) : KotlinDeclarationProviderFactory {
    private val indexData: IndexData = computeIndex(
        project = project,
        sourceKtFiles = sourceKtFiles,
        binaryRoots = binaryRoots,
        sharedBinaryRoots = sharedBinaryRoots,
        skipBuiltins = skipBuiltins,
        shouldBuildStubsForBinaryLibraries = shouldBuildStubsForBinaryLibraries,
    )

    private val index: KotlinStandaloneDeclarationIndex
        get() = indexData.index

    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KaModule?): KotlinDeclarationProvider {
        return KotlinStandaloneDeclarationProvider(index, scope, contextualModule, environment, shouldComputeBinaryLibraryPackageSets)
    }

    fun getAdditionalCreatedKtFiles(): List<KtFile> {
        return indexData.fakeKtFiles
    }

    fun getAllKtClasses(): List<KtClassOrObject> = index.classMap.values.flattenTo(mutableListOf())

    fun getDirectInheritorCandidates(baseClassName: Name): Set<KtClassOrObject> =
        index.classesBySupertypeName[baseClassName].orEmpty()

    fun getInheritableTypeAliases(aliasedName: Name): Set<KtTypeAlias> =
        index.inheritableTypeAliasesByAliasedName[aliasedName].orEmpty()
}

private class IndexData(val fakeKtFiles: List<KtFile>, val index: KotlinStandaloneDeclarationIndex)

private class KotlinStandaloneDeclarationIndexImpl : KotlinStandaloneDeclarationIndex {
    override val facadeFileMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    override val multiFileClassPartMap: MutableMap<FqName, MutableSet<KtFile>> = mutableMapOf()
    override val scriptMap: MutableMap<FqName, MutableSet<KtScript>> = mutableMapOf()
    override val classMap: MutableMap<FqName, MutableSet<KtClassOrObject>> = mutableMapOf()
    override val typeAliasMap: MutableMap<FqName, MutableSet<KtTypeAlias>> = mutableMapOf()
    override val topLevelFunctionMap: MutableMap<FqName, MutableSet<KtNamedFunction>> = mutableMapOf()
    override val topLevelPropertyMap: MutableMap<FqName, MutableSet<KtProperty>> = mutableMapOf()
    override val classesBySupertypeName: MutableMap<Name, MutableSet<KtClassOrObject>> = mutableMapOf()
    override val inheritableTypeAliasesByAliasedName: MutableMap<Name, MutableSet<KtTypeAlias>> = mutableMapOf()

    fun indexFile(file: KtFile) {
        if (!file.hasTopLevelCallables()) return
        facadeFileMap.computeIfAbsent(file.packageFqName) {
            mutableSetOf()
        }.add(file)
    }

    fun indexScript(script: KtScript) {
        scriptMap.computeIfAbsent(script.fqName) {
            mutableSetOf()
        }.add(script)
    }

    fun indexClassOrObject(classOrObject: KtClassOrObject) {
        classOrObject.getClassId()?.let { classId ->
            classMap.computeIfAbsent(classId.packageFqName) {
                mutableSetOf()
            }.add(classOrObject)
        }

        classOrObject.getSuperNames().forEach { superName ->
            classesBySupertypeName
                .computeIfAbsent(Name.identifier(superName)) { mutableSetOf() }
                .add(classOrObject)
        }
    }

    fun indexTypeAlias(typeAlias: KtTypeAlias) {
        typeAlias.getClassId()?.let { classId ->
            typeAliasMap.computeIfAbsent(classId.packageFqName) {
                mutableSetOf()
            }.add(typeAlias)
        }

        val typeElement = typeAlias.getTypeReference()?.typeElement ?: return

        findInheritableSimpleNames(typeElement).forEach { expandedName ->
            inheritableTypeAliasesByAliasedName
                .computeIfAbsent(Name.identifier(expandedName)) { mutableSetOf() }
                .add(typeAlias)
        }
    }

    fun processMultifileClassStub(fileStub: KotlinFileStubImpl) {
        val ktFile: KtFile = fileStub.psi

        val partNames = fileStub.facadePartSimpleNames ?: return
        val packageFqName = fileStub.getPackageFqName()
        for (partName in partNames) {
            val multiFileClassPartFqName: FqName = packageFqName.child(Name.identifier(partName))
            multiFileClassPartMap.computeIfAbsent(multiFileClassPartFqName) { mutableSetOf() }.add(ktFile)
        }
    }

    fun indexNamedFunction(function: KtNamedFunction) {
        if (!function.isTopLevel) return
        val packageFqName = function.containingKtFile.packageFqName
        topLevelFunctionMap.computeIfAbsent(packageFqName) {
            mutableSetOf()
        }.add(function)
    }

    fun indexProperty(property: KtProperty) {
        if (!property.isTopLevel) return
        val packageFqName = property.containingKtFile.packageFqName
        topLevelPropertyMap.computeIfAbsent(packageFqName) {
            mutableSetOf()
        }.add(property)
    }

    fun indexStubRecursively(stub: StubElement<*>): Unit = when (stub) {
        is KotlinFileStubImpl -> {
            val file = stub.psi
            indexFile(file)
            processMultifileClassStub(stub)

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
        is KotlinPlaceHolderStubImpl if (stub.stubType == KtStubElementTypes.CLASS_BODY) -> {
            stub.childrenStubs
                .filterIsInstance<KotlinClassOrObjectStub<*>>()
                .forEach(::indexStubRecursively)
        }

        else -> Unit
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

private fun computeIndex(
    project: Project,
    sourceKtFiles: Collection<KtFile>,
    binaryRoots: List<VirtualFile>,
    sharedBinaryRoots: List<VirtualFile>,
    skipBuiltins: Boolean,
    shouldBuildStubsForBinaryLibraries: Boolean,
): IndexData {
    val index = KotlinStandaloneDeclarationIndexImpl()

    val psiManager = PsiManager.getInstance(project)
    val builtInDecompiler = KotlinBuiltInDecompiler()
    val createdFakeKtFiles = mutableListOf<KtFile>()

    fun createFileStub(virtualFile: VirtualFile): KotlinFileStubImpl? {
        val fileContent = FileContentImpl.createByFile(virtualFile, project)
        return builtInDecompiler.stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl
    }

    class KtClassFileViewProvider(
        psiManager: PsiManager,
        virtualFile: VirtualFile,
    ) : SingleRootFileViewProvider(psiManager, virtualFile, true, KotlinLanguage.INSTANCE)

    @OptIn(KaImplementationDetail::class)
    fun registerStub(stub: KotlinFileStubImpl, virtualFile: VirtualFile, isSharedStub: Boolean): KotlinFileStubImpl {
        val resultStub = if (isSharedStub) {
            if (stub.psi != null) {
                error("Shared stub cannot have psi as it leads to a project leak")
            }

            cloneStubRecursively(
                originalStub = stub,
                copyParentStub = null,
                buffer = UnsyncByteArrayOutputStream(),
                storage = StringEnumerator(),
            ) as KotlinFileStubImpl
        } else {
            stub
        }

        val fileViewProvider = KtClassFileViewProvider(psiManager, virtualFile)
        val fakeFile = object : KtFile(fileViewProvider, isCompiled = true), SmartPointerIncompatiblePsiFile {
            override fun getStub(): KotlinFileStub? = resultStub
            override val greenStub: KotlinFileStub? get() = resultStub
            override fun isPhysical() = false
        }

        resultStub.psi = fakeFile
        createdFakeKtFiles.add(fakeFile)
        return resultStub
    }

    fun loadBuiltIns(): Collection<KotlinFileStubImpl> {
        val cacheService = ApplicationManager.getApplication().serviceOrNull<KotlinFakeClsStubsCache>()
        return BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().mapNotNull { virtualFile ->
            val stub = if (cacheService != null)
                cacheService.processBuiltinsFile(virtualFile, ::createFileStub)
            else
                createFileStub(virtualFile)

            stub?.let {
                registerStub(it, virtualFile, isSharedStub = cacheService != null)
            }
        }
    }

    fun buildStubByVirtualFile(
        file: VirtualFile,
        binaryClassCache: ClsKotlinBinaryClassCache,
    ): KotlinFileStubImpl? {
        val fileContent = FileContentImpl.createByFile(file)
        val fileType = fileContent.fileType
        val stubBuilder = when (fileType) {
            JavaClassFileType.INSTANCE if binaryClassCache.isKotlinJvmCompiledFile(file, fileContent.content) -> {
                KotlinClsStubBuilder()
            }

            KotlinBuiltInFileType -> {
                builtInDecompiler.stubBuilder
            }

            KlibMetaFileType -> K2KotlinNativeMetadataDecompiler().stubBuilder
            else -> return null
        }

        return stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl
    }

    fun collectStubsFromBinaryRoot(
        binaryRoot: VirtualFile,
        binaryClassCache: ClsKotlinBinaryClassCache,
    ): Map<VirtualFile, KotlinFileStubImpl> =
        buildMap {
            VfsUtilCore.visitChildrenRecursively(binaryRoot, object : VirtualFileVisitor<Void>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory) {
                        val stub = buildStubByVirtualFile(file, binaryClassCache) ?: return true
                        put(file, stub)
                    }
                    return true
                }
            })
        }

    fun processCollectedBinaryStubs(stubs: Map<VirtualFile, KotlinFileStubImpl>, isSharedStubs: Boolean) {
        val (builtinEntries, otherEntries) = stubs.entries.partition { entry -> entry.key.fileType == KotlinBuiltInFileType }

        fun process(entries: List<Map.Entry<VirtualFile, KotlinFileStubImpl>>) {
            entries.forEach { entry ->
                val stub = registerStub(entry.value, entry.key, isSharedStubs)
                index.indexStubRecursively(stub)
            }
        }

        process(otherEntries)

        // Due to KT-78748, we have to index builtin declarations last so that class declarations are preferred. Note that this currently
        // only affects Analysis API tests, since production Standalone doesn't index binary declarations as stubs.
        process(builtinEntries)
    }

    class KtDeclarationRecorder : KtVisitorVoid() {
        override fun visitElement(element: PsiElement) {
            element.acceptChildren(this)
        }

        override fun visitKtFile(file: KtFile) {
            index.indexFile(file)
            super.visitKtFile(file)
        }

        override fun visitScript(script: KtScript) {
            index.indexScript(script)
            super.visitScript(script)
        }

        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
            index.indexClassOrObject(classOrObject)
            super.visitClassOrObject(classOrObject)
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            index.indexTypeAlias(typeAlias)
            super.visitTypeAlias(typeAlias)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            index.indexNamedFunction(function)
            super.visitNamedFunction(function)
        }

        override fun visitProperty(property: KtProperty) {
            index.indexProperty(property)
            super.visitProperty(property)
        }
    }

    val recorder = KtDeclarationRecorder()

    // Indexing built-ins
    if (!skipBuiltins) {
        loadBuiltIns().forEach {
            index.indexStubRecursively(it)
        }
    }

    // We only need to index binary roots if we deserialize compiled symbols from stubs. When deserializing from class files, we don't
    // need these symbols in the declaration provider.
    if (shouldBuildStubsForBinaryLibraries) {
        val binaryClassCache = ClsKotlinBinaryClassCache.getInstance()

        for (root in sharedBinaryRoots) {
            KotlinFakeClsStubsCache.processAdditionalRoot(root) { additionalRoot ->
                collectStubsFromBinaryRoot(additionalRoot, binaryClassCache)
            }?.let { processCollectedBinaryStubs(it, isSharedStubs = true) }
        }

        // In contrast to `sharedBinaryRoots`, which are shared between many different projects (e.g. the Kotlin stdlib), `binaryRoots`
        // come from binary libraries which are specific to the project. Caching them in `KotlinFakeClsStubsCache` won't have any
        // performance advantage.
        binaryRoots
            .map { collectStubsFromBinaryRoot(it, binaryClassCache) }
            .forEach { processCollectedBinaryStubs(it, isSharedStubs = false) }

        for (file in sourceKtFiles) {
            if (!file.isCompiled) continue

            val stub = buildStubByVirtualFile(file.virtualFile, binaryClassCache)

            // Only files for which stub exists should be indexed, so some synthetic classes should be ignored.
            // This behavior is closer to real indices.
            if (stub != null) {
                stub.psi = file
                index.processMultifileClassStub(stub)

                file.accept(recorder)
            }
        }
    }

    sourceKtFiles.forEach { file ->
        if (!shouldBuildStubsForBinaryLibraries || !file.isCompiled) {
            file.accept(recorder)
        }
    }

    return IndexData(
        fakeKtFiles = createdFakeKtFiles,
        index = index,
    )
}

/**
 * Test application service to store stubs of shared between tests libraries.
 *
 * Otherwise, each test would start indexing of stdlib from scratch,
 * and under the lock which makes tests extremely slow
 *
 * **Note**: shared stubs **MUST NOT** store psi
 */
internal class KotlinFakeClsStubsCache {
    private val fakeFileClsStubs = ConcurrentHashMap<String, Map<VirtualFile, KotlinFileStubImpl>>()
    private val fakeBuiltInsClsStubs = ConcurrentHashMap<VirtualFile, KotlinFileStubImpl?>()

    fun processBuiltinsFile(
        root: VirtualFile,
        buildStub: (VirtualFile) -> KotlinFileStubImpl?,
    ): KotlinFileStubImpl? = fakeBuiltInsClsStubs.computeIfAbsent(root, buildStub)

    companion object {
        fun processAdditionalRoot(
            root: VirtualFile,
            storage: (VirtualFile) -> Map<VirtualFile, KotlinFileStubImpl>,
        ): Map<VirtualFile, KotlinFileStubImpl>? {
            val service = ApplicationManager.getApplication().serviceOrNull<KotlinFakeClsStubsCache>() ?: return null
            return service.fakeFileClsStubs.computeIfAbsent(root.path) { _ ->
                storage(root)
            }
        }
    }
}

class KotlinStandaloneDeclarationProviderMerger(private val project: Project) : KotlinDeclarationProviderMerger {
    override fun merge(providers: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
        providers.mergeSpecificProviders<_, KotlinStandaloneDeclarationProvider>(KotlinCompositeDeclarationProvider.factory) { targetProviders ->
            val combinedScope = GlobalSearchScope.union(targetProviders.map { it.scope })
            project.createDeclarationProvider(combinedScope, contextualModule = null).apply {
                check(this is KotlinStandaloneDeclarationProvider) {
                    "`KotlinStandaloneDeclarationProvider` can only be merged into a combined declaration provider of the same type."
                }
            }
        }
}

/**
 * Returns a copy of [originalStub].
 *
 * @see KotlinFakeClsStubsCache
 */
private fun <T : PsiElement> cloneStubRecursively(
    originalStub: StubElement<T>,
    copyParentStub: StubElement<*>?,
    buffer: UnsyncByteArrayOutputStream,
    storage: AbstractStringEnumerator,
): StubElement<*> {
    buffer.reset()

    // Some specific elements are covered here as they widely used and has additional logic inside `serialize`,
    // to it is an optimization
    val copyStub = when (originalStub) {
        is KotlinUserTypeStubImpl -> KotlinUserTypeStubImpl(
            copyParentStub,
            originalStub.upperBound,
            originalStub.abbreviatedType,
        )

        is KotlinNameReferenceExpressionStubImpl -> KotlinNameReferenceExpressionStubImpl(
            copyParentStub,
            StringRef.fromString(originalStub.referencedName),
            originalStub.isClassRef,
        )

        is PsiFileStub -> {
            val serializer = originalStub.type
            serializer.serialize(originalStub, StubOutputStream(buffer, storage))
            serializer.deserialize(StubInputStream(buffer.toInputStream(), storage), copyParentStub)
        }

        else -> {
            val serializer = originalStub.stubType
            serializer.serialize(originalStub, StubOutputStream(buffer, storage))
            serializer.deserialize(StubInputStream(buffer.toInputStream(), storage), copyParentStub)
        }
    }

    for (originalChild in originalStub.childrenStubs) {
        cloneStubRecursively(originalStub = originalChild, copyParentStub = copyStub, buffer = buffer, storage = storage)
    }

    return copyStub
}

private class StringEnumerator : AbstractStringEnumerator {
    private val values = HashMap<String, Int>()
    private val strings = mutableListOf<String>()

    override fun enumerate(value: String?): Int {
        if (value == null) return 0

        return values.getOrPut(value) {
            strings += value
            values.size + 1
        }
    }

    override fun valueOf(idx: Int): String? = if (idx == 0) null else strings[idx - 1]

    override fun markCorrupted(): Unit = shouldNotBeCalled()
    override fun close(): Unit = shouldNotBeCalled()
    override fun isDirty(): Boolean = shouldNotBeCalled()
    override fun force(): Unit = shouldNotBeCalled()
}