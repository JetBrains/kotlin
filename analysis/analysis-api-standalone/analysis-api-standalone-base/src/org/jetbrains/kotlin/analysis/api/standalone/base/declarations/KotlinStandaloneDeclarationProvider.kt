/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.StubFileElementType
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.StringRef
import com.intellij.util.io.UnsyncByteArrayOutputStream
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.SmartPointerIncompatiblePsiFile
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
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
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.util.concurrent.ConcurrentHashMap

class KotlinStandaloneDeclarationProvider internal constructor(
    private val index: KotlinStandaloneDeclarationIndex,
    val scope: GlobalSearchScope,
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

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = true

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String> =
        buildPackageNamesSetFrom(index.classMap.keys, index.typeAliasMap.keys)

    override val hasSpecificCallablePackageNamesComputation: Boolean get() = true

    override fun computePackageNamesWithTopLevelCallables(): Set<String> =
        buildPackageNamesSetFrom(index.topLevelPropertyMap.keys, index.topLevelFunctionMap.keys)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun buildPackageNamesSetFrom(vararg fqNameSets: Set<FqName>): Set<String> =
        buildSet {
            for (fqNameSet in fqNameSets) {
                fqNameSet.mapTo(this, FqName::asString)
            }
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
 * shared libraries like the Kotlin stdlib.
 */
class KotlinStandaloneDeclarationProviderFactory(
    private val project: Project,
    sourceKtFiles: Collection<KtFile>,
    binaryRoots: List<VirtualFile> = emptyList(),
    sharedBinaryRoots: List<VirtualFile> = emptyList(),
    skipBuiltins: Boolean = false,
    shouldBuildStubsForBinaryLibraries: Boolean = false,
) : KotlinDeclarationProviderFactory {

    private val index = KotlinStandaloneDeclarationIndex()

    private val psiManager = PsiManager.getInstance(project)
    private val builtInDecompiler = KotlinBuiltInDecompiler()
    private val createdFakeKtFiles = mutableListOf<KtFile>()

    private fun loadBuiltIns(): Collection<KotlinFileStubImpl> {
        val cacheService = ApplicationManager.getApplication().serviceOrNull<KotlinFakeClsStubsCache>()
        return BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().mapNotNull { virtualFile ->
            val stub = if (cacheService != null)
                cacheService.processBuiltinsFile(virtualFile, this::createFileStub)
            else
                createFileStub(virtualFile)

            stub?.let {
                registerStub(it, virtualFile, isSharedStub = cacheService != null)
            }
        }
    }

    private fun createFileStub(virtualFile: VirtualFile): KotlinFileStubImpl? {
        val fileContent = FileContentImpl.createByFile(virtualFile, project)
        return builtInDecompiler.stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl
    }

    @OptIn(KaImplementationDetail::class)
    private fun registerStub(stub: KotlinFileStubImpl, virtualFile: VirtualFile, isSharedStub: Boolean): KotlinFileStubImpl {
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
            indexClassOrObject(classOrObject)
            super.visitClassOrObject(classOrObject)
        }

        override fun visitTypeAlias(typeAlias: KtTypeAlias) {
            indexTypeAlias(typeAlias)
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

    private fun indexClassOrObject(classOrObject: KtClassOrObject) {
        addToClassMap(classOrObject)
        indexSupertypeNames(classOrObject)
    }

    private fun addToClassMap(classOrObject: KtClassOrObject) {
        classOrObject.getClassId()?.let { classId ->
            index.classMap.computeIfAbsent(classId.packageFqName) {
                mutableSetOf()
            }.add(classOrObject)
        }
    }

    private fun indexSupertypeNames(classOrObject: KtClassOrObject) {
        classOrObject.getSuperNames().forEach { superName ->
            index.classesBySupertypeName
                .computeIfAbsent(Name.identifier(superName)) { mutableSetOf() }
                .add(classOrObject)
        }
    }

    private fun indexTypeAlias(typeAlias: KtTypeAlias) {
        addToTypeAliasMap(typeAlias)
        indexTypeAliasDefinition(typeAlias)
    }

    private fun addToTypeAliasMap(typeAlias: KtTypeAlias) {
        typeAlias.getClassId()?.let { classId ->
            index.typeAliasMap.computeIfAbsent(classId.packageFqName) {
                mutableSetOf()
            }.add(typeAlias)
        }
    }

    private fun indexTypeAliasDefinition(typeAlias: KtTypeAlias) {
        val typeElement = typeAlias.getTypeReference()?.typeElement ?: return

        findInheritableSimpleNames(typeElement).forEach { expandedName ->
            index.inheritableTypeAliasesByAliasedName
                .computeIfAbsent(Name.identifier(expandedName)) { mutableSetOf() }
                .add(typeAlias)
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
        if (!skipBuiltins) {
            loadBuiltIns().forEach {
                processStub(it)
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

                // Special handling for builtins is required as normally they are indexed by [loadBuiltIns],
                // so [buildStubByVirtualFile] skips them explicitly, but actually stubs for them exist
                val stub = buildStubByVirtualFile(file.virtualFile, binaryClassCache, preserveBuiltins = skipBuiltins)

                // Only files for which stub exists should be indexed, so some synthetic classes should be ignored.
                // This behavior is closer to real indices.
                if (stub != null) {
                    stub.psi = file
                    processMultifileClassStub(stub)

                    file.accept(recorder)
                }
            }
        }

        sourceKtFiles.forEach { file ->
            if (!shouldBuildStubsForBinaryLibraries || !file.isCompiled) {
                file.accept(recorder)
            }
        }
    }

    private fun indexStub(stub: StubElement<*>) {
        when (stub) {
            is KotlinClassStubImpl -> {
                indexClassOrObject(stub.psi)
                // member functions and properties
                stub.childrenStubs.forEach(::indexStub)
            }
            is KotlinObjectStubImpl -> {
                indexClassOrObject(stub.psi)
                // member functions and properties
                stub.childrenStubs.forEach(::indexStub)
            }
            is KotlinTypeAliasStubImpl -> indexTypeAlias(stub.psi)
            is KotlinFunctionStubImpl -> addToFunctionMap(stub.psi)
            is KotlinPropertyStubImpl -> addToPropertyMap(stub.psi)
            is KotlinPlaceHolderStubImpl -> {
                if (stub.stubType == KtStubElementTypes.CLASS_BODY) {
                    stub.childrenStubs.filterIsInstance<KotlinClassOrObjectStub<*>>().forEach(::indexStub)
                }
            }
        }
    }

    private fun processMultifileClassStub(ktFileStub: KotlinFileStubImpl) {
        val ktFile: KtFile = ktFileStub.psi

        val partNames = ktFileStub.facadePartSimpleNames ?: return
        val packageFqName = ktFileStub.getPackageFqName()
        for (partName in partNames) {
            val multiFileClassPartFqName: FqName = packageFqName.child(Name.identifier(partName))
            index.multiFileClassPartMap.computeIfAbsent(multiFileClassPartFqName) { mutableSetOf() }.add(ktFile)
        }
    }

    private fun processStub(ktFileStub: KotlinFileStubImpl) {
        addToFacadeFileMap(ktFileStub.psi)
        processMultifileClassStub(ktFileStub)

        // top-level functions and properties, built-in classes
        ktFileStub.childrenStubs.forEach(::indexStub)
    }

    private fun collectStubsFromBinaryRoot(
        binaryRoot: VirtualFile,
        binaryClassCache: ClsKotlinBinaryClassCache,
    ): Map<VirtualFile, KotlinFileStubImpl> =
        buildMap {
            VfsUtilCore.visitChildrenRecursively(binaryRoot, object : VirtualFileVisitor<Void>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory) {
                        val stub = buildStubByVirtualFile(file, binaryClassCache, preserveBuiltins = false) ?: return true
                        put(file, stub)
                    }
                    return true
                }
            })
        }

    private fun buildStubByVirtualFile(
        file: VirtualFile,
        binaryClassCache: ClsKotlinBinaryClassCache,
        preserveBuiltins: Boolean,
    ): KotlinFileStubImpl? {
        val fileContent = FileContentImpl.createByFile(file)
        val fileType = fileContent.fileType
        val stubBuilder = when (fileType) {
            JavaClassFileType.INSTANCE if binaryClassCache.isKotlinJvmCompiledFile(file, fileContent.content) -> {
                KotlinClsStubBuilder()
            }

            KotlinBuiltInFileType if preserveBuiltins || file.extension != BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION -> {
                builtInDecompiler.stubBuilder
            }

            KlibMetaFileType -> K2KotlinNativeMetadataDecompiler().stubBuilder
            else -> return null
        }

        return stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl
    }

    private fun processCollectedBinaryStubs(stubs: Map<VirtualFile, KotlinFileStubImpl>, isSharedStubs: Boolean) {
        stubs.forEach { entry ->
            val stub = registerStub(entry.value, entry.key, isSharedStubs)
            processStub(stub)
        }
    }

    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KaModule?): KotlinDeclarationProvider {
        return KotlinStandaloneDeclarationProvider(index, scope)
    }

    fun getAdditionalCreatedKtFiles(): List<KtFile> {
        return createdFakeKtFiles
    }

    fun getAllKtClasses(): List<KtClassOrObject> = index.classMap.values.flattenTo(mutableListOf())

    fun getDirectInheritorCandidates(baseClassName: Name): Set<KtClassOrObject> =
        index.classesBySupertypeName[baseClassName].orEmpty()

    fun getInheritableTypeAliases(aliasedName: Name): Set<KtTypeAlias> =
        index.inheritableTypeAliasesByAliasedName[aliasedName].orEmpty()
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
            storage: (VirtualFile) -> Map<VirtualFile, KotlinFileStubImpl>
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
            StringRef.fromString(originalStub.getReferencedName()),
            originalStub.isClassRef,
        )

        is PsiFileStub -> {
            val serializer = originalStub.type as StubFileElementType<PsiFileStub<PsiFile>>
            serializer.serialize(originalStub as PsiFileStub<PsiFile>, StubOutputStream(buffer, storage))
            serializer.deserialize(StubInputStream(buffer.toInputStream(), storage), copyParentStub)
        }

        else -> {
            val serializer = originalStub.stubType as IStubElementType<StubElement<*>, PsiElement>
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