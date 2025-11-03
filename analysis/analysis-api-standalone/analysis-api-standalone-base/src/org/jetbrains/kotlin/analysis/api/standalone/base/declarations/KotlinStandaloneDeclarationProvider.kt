/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.projectStructure.*
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
        index.classesByClassId[classId]
            ?.filter { it.inScope }
            ?: emptyList()

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        index.typeAliasesByClassId[classId]
            ?.filter { it.inScope }
            ?: emptyList()

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        val classifiers = index.classLikeDeclarationsByPackage[packageFqName].orEmpty()
        return classifiers
            .filter { it.inScope }
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        val callables = index.topLevelCallablesByPackage[packageFqName].orEmpty()
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
        addPackageNamesInScope(index.classLikeDeclarationsByPackage)
        addPackageNamesInScope(index.topLevelCallablesByPackage)
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

        val binaryVirtualFiles = module.binaryVirtualFiles

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
        index.topLevelPropertiesByCallableId[callableId]
            ?.filter { it.inScope }
            ?: emptyList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        index.topLevelFunctionsByCallableId[callableId]
            ?.filter { it.inScope }
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
    private val indexData: KotlinStandaloneIndexBuilder.IndexData = KotlinStandaloneIndexBuilder(
        project = project,
        shouldBuildStubsForDecompiledFiles = shouldBuildStubsForBinaryLibraries,
        postponeIndexing = postponeIndexing,
    ) {
        collectSourceFiles(sourceKtFiles)

        if (!skipBuiltins) {
            collectDecompiledFilesFromBuiltins()
        }

        // We only need to index binary roots if we deserialize compiled symbols from stubs. When deserializing from class files, we don't
        // need these symbols in the declaration provider.
        if (shouldBuildStubsForBinaryLibraries) {
            for (root in sharedBinaryRoots) {
                collectDecompiledFilesFromBinaryRoot(root, isSharedRoot = true)
            }

            for (root in binaryRoots) {
                collectDecompiledFilesFromBinaryRoot(root, isSharedRoot = false)
            }
        }
    }

    private val index: KotlinStandaloneDeclarationIndex
        get() = indexData.index

    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KaModule?): KotlinDeclarationProvider {
        return KotlinStandaloneDeclarationProvider(index, scope, contextualModule, environment, shouldComputeBinaryLibraryPackageSets)
    }

    fun getAdditionalCreatedKtFiles(): List<KtFile> {
        return indexData.fakeKtFiles
    }

    fun getAllKtClasses(): List<KtClassOrObject> = index.classesByClassId.values.flattenTo(mutableListOf())

    fun getDirectInheritorCandidates(baseClassName: Name): Set<KtClassOrObject> =
        index.classesBySupertypeName[baseClassName].orEmpty()

    fun getInheritableTypeAliases(aliasedName: Name): Set<KtTypeAlias> =
        index.inheritableTypeAliasesByAliasedName[aliasedName].orEmpty()
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
