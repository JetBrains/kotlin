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
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.file.deepCopy
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo

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
 * @param postponeIndexing Whether to postpone indexing until the first access.
 *  This is useful for tests to reduce the startup time and potentially avoid redundant indexing (which might be heavy, especially if stubs are used).
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
    postponeIndexing: Boolean = false,
) : KotlinDeclarationProviderFactory {
    private val indexData: IndexData = computeIndex(
        project = project,
        sourceKtFiles = sourceKtFiles,
        binaryRoots = binaryRoots,
        sharedBinaryRoots = sharedBinaryRoots,
        skipBuiltins = skipBuiltins,
        shouldBuildStubsForBinaryLibraries = shouldBuildStubsForBinaryLibraries,
        postponeIndexing = postponeIndexing,
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

private class IndexableFile(
    /**
     * The virtual file of the file that is being indexed.
     */
    val virtualFile: VirtualFile,

    /**
     * The [KtFile] associated with the [virtualFile].
     */
    val ktFile: KtFile,

    /**
     * Whether the file is shared between multiple projects.
     */
    val isShared: Boolean,
) {
    override fun equals(other: Any?): Boolean = this === other || other is IndexableFile && virtualFile == other.virtualFile
    override fun hashCode(): Int = virtualFile.hashCode()
    override fun toString(): String = virtualFile.toString()
}

private fun MutableSet<IndexableFile>.collectDecompiledFilesFromBinaryRoot(
    psiManager: PsiManager,
    binaryRoot: VirtualFile,
    isSharedRoot: Boolean,
) {
    VfsUtilCore.visitChildrenRecursively(binaryRoot, object : VirtualFileVisitor<Void>() {
        override fun visitFile(file: VirtualFile): Boolean {
            if (!file.isDirectory) {
                val ktFile = psiManager.findFile(file) as? KtFile
                // Synthetic class parts are not supposed to be indexed to avoid duplicates
                // The information about virtual files are already cached after the previous line
                if (ktFile != null && !ClsClassFinder.isMultifileClassPartFile(file)) {
                    add(IndexableFile(file, ktFile, isShared = isSharedRoot))
                }
            }

            return true
        }
    })
}

private fun computeIndex(
    project: Project,
    sourceKtFiles: Collection<KtFile>,
    binaryRoots: List<VirtualFile>,
    sharedBinaryRoots: List<VirtualFile>,
    skipBuiltins: Boolean,
    shouldBuildStubsForBinaryLibraries: Boolean,
    postponeIndexing: Boolean,
): IndexData {
    val rawIndex = KotlinStandaloneDeclarationIndexImpl()

    val psiManager = PsiManager.getInstance(project)
    val cacheService = ApplicationManager.getApplication().serviceOrNull<KotlinFakeClsStubsCache>()

    // Synchronization is not needed since either this code is executed in a single thread right away or guarded by the next synchronized `lazyIndex` property
    val setStubTreeMethod by lazy(LazyThreadSafetyMode.NONE) {
        val setStubTreeMethodName = "setStubTree"

        PsiFileImpl::class
            .java
            .declaredMethods
            .find { it.name == setStubTreeMethodName && it.parameterCount == 1 }
            ?.also { it.isAccessible = true }
            ?: error("'${PsiFileImpl::class.simpleName}.$setStubTreeMethodName' method is not found")
    }

    fun indexStubRecursively(indexableFile: IndexableFile) {
        val virtualFile = indexableFile.virtualFile
        val ktFile = indexableFile.ktFile

        // Stub calculation
        if (indexableFile.isShared && cacheService != null) {
            val stub = cacheService.getOrBuildStub(virtualFile) {
                ktFile.forcedStub
            }

            if (stub.psi != ktFile) {
                @OptIn(KtImplementationDetail::class)
                val clonedStub = stub.deepCopy()

                // A hack to avoid costly stub builder execution
                setStubTreeMethod.invoke(ktFile, clonedStub)
            }
        }

        val stub = ktFile.forcedStub
        rawIndex.indexStubRecursively(stub)
    }

    // We only need to index binary roots if we deserialize compiled symbols from stubs. When deserializing from class files, we don't
    // need these symbols in the declaration provider.
    val decompiledFilesFromBinaryRoots: Set<IndexableFile> = if (shouldBuildStubsForBinaryLibraries) {
        buildSet {
            for (root in sharedBinaryRoots) {
                collectDecompiledFilesFromBinaryRoot(psiManager, root, isSharedRoot = true)
            }

            for (root in binaryRoots) {
                collectDecompiledFilesFromBinaryRoot(psiManager, root, isSharedRoot = false)
            }
        }
    } else {
        emptySet()
    }

    val decompiledFilesFromBuiltins = if (!skipBuiltins) {
        BuiltinsVirtualFileProvider.getInstance()
            .getBuiltinVirtualFiles()
            .mapNotNull { virtualFile ->
                (psiManager.findFile(virtualFile) as? KtFile)?.let {
                    IndexableFile(virtualFile, it, isShared = true)
                }
            }
    } else {
        emptyList()
    }

    // indexing
    fun buildIndex(): KotlinStandaloneDeclarationIndexImpl {
        decompiledFilesFromBuiltins.forEach(::indexStubRecursively)

        val (decompiledBuiltinsFilesFromBinaryRoots, decompiledFilesFromOtherFiles) = decompiledFilesFromBinaryRoots.partition { entry ->
            entry.virtualFile.fileType == KotlinBuiltInFileType
        }

        decompiledFilesFromOtherFiles.forEach(::indexStubRecursively)

        // Due to KT-78748, we have to index builtin declarations last so that class declarations are preferred. Note that this currently
        // only affects Analysis API tests, since production Standalone doesn't index binary declarations as stubs.
        decompiledBuiltinsFilesFromBinaryRoots.forEach(::indexStubRecursively)

        val astBasedIndexer = rawIndex.AstBasedIndexer()

        for (file in sourceKtFiles) when {
            !file.isCompiled -> {
                val stub = file.stub
                if (stub != null) {
                    rawIndex.indexStubRecursively(stub)
                } else {
                    file.accept(astBasedIndexer)
                }
            }

            shouldBuildStubsForBinaryLibraries -> rawIndex.indexStubRecursively(file.forcedStub)
        }

        return rawIndex
    }

    val initializedIndex = if (postponeIndexing) {
        KotlinStandaloneLazyDeclarationIndexImpl(lazy(::buildIndex))
    } else {
        buildIndex()
    }

    return IndexData(
        fakeKtFiles = decompiledFilesFromBuiltins.map(IndexableFile::ktFile) + decompiledFilesFromBinaryRoots.map(IndexableFile::ktFile),
        index = initializedIndex,
    )
}

/**
 * [PsiFileImpl.getGreenStubTree] is cheaper if it is available since it doesn't require computing the AST tree
 */
private val KtFile.forcedStub: KotlinFileStubImpl
    get() {
        val stubTree = greenStubTree ?: calcStubTree()
        return stubTree.root as KotlinFileStubImpl
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
