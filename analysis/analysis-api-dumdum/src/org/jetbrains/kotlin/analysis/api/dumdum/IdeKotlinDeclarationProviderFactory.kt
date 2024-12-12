// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

@file:OptIn(UnsafeCastFunction::class)

package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.KotlinTopLevelCallableByPackageShortNameIndex
import org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.KotlinTopLevelClassLikeDeclarationByPackageShortNameIndex
import org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.getNamesInPackage
import org.jetbrains.kotlin.analysis.api.dumdum.index.*
import org.jetbrains.kotlin.analysis.api.dumdum.stubindex.*
import org.jetbrains.kotlin.analysis.api.platform.declarations.*
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class IdeKotlinDeclarationProviderFactory(
    private val project: Project,
    private val stubIndex: StubIndex,
    private val fileBasedIndex: FileBasedIndex,
) : KotlinDeclarationProviderFactory {
    override fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KaModule?): KotlinDeclarationProvider {
        val mainProvider = IdeKotlinDeclarationProvider(
            project = project,
            scope = scope,
            stubIndex = stubIndex,
            fileBasedIndex = fileBasedIndex,
            contextualModule = contextualModule
        )

//        if (contextualModule is KaSourceModuleForOutsider) {
//            val fakeKtFile = PsiManager.getInstance(contextualModule.project).findFile(contextualModule.fakeVirtualFile)
//            if (fakeKtFile is KtFile) {
//                val providerForFake = KotlinFileBasedDeclarationProvider(fakeKtFile)
//                return KotlinCompositeDeclarationProvider.create(listOf(providerForFake, mainProvider))
//            }
//        }

        return mainProvider
    }
}

internal class IdeKotlinDeclarationProviderMerger(
    private val project: Project,
    private val stubIndex: StubIndex,
    private val fileBasedIndex: FileBasedIndex,
) : KotlinDeclarationProviderMerger {
    override fun merge(providers: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
        providers.mergeSpecificProviders<_, IdeKotlinDeclarationProvider>(KotlinCompositeDeclarationProvider.factory) { targetProviders ->
            IdeKotlinDeclarationProvider(
                project,
                KotlinGlobalSearchScopeMerger.getInstance(project).union(targetProviders.map { it.scope }),
                contextualModule = null,
                fileBasedIndex = fileBasedIndex,
                stubIndex = stubIndex
            )
        }
}

inline fun <T> processElementsAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    block()

private class IdeKotlinDeclarationProvider(
    private val project: Project,
    val scope: GlobalSearchScope,
    val stubIndex: StubIndex,
    val fileBasedIndex: FileBasedIndex,
    private val contextualModule: KaModule?,
) : KotlinDeclarationProvider {
    private val psiManager = PsiManager.getInstance(project)

    private inline fun <IndexKey : Any, reified Psi : PsiElement> firstMatchingOrNull(
        stubKey: StubIndexKey<IndexKey, Psi>,
        key: IndexKey,
        crossinline filter: (Psi) -> Boolean = { true },
    ): Psi? {
        var result: Psi? = null
        processElementsAndMeasure(stubKey, log) {
            stubIndex.processElements(stubKey, key, project, scope, Psi::class.java) { candidate ->
                ProgressManager.checkCanceled()
                if (filter(candidate)) {
                    result = candidate
                    return@processElements false // do not continue searching over PSI
                }
                return@processElements true
            }
        }
        return result
    }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        val classOrObject = firstMatchingOrNull(KotlinFullClassNameIndex.indexKey, key = classId.asStringForIndexes()) { candidate ->
            candidate.getClassId() == classId
        }
        val typeAlias = getTypeAliasByClassId(classId)
        if (classOrObject != null && typeAlias != null) {
            if (scope.compare(classOrObject.containingFile.virtualFile, typeAlias.containingFile.virtualFile) < 0) {
                return typeAlias
            }
        }
        return classOrObject ?: typeAlias
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        KotlinFullClassNameIndex.getAllElements(
            stubIndex = stubIndex,
            key = classId.asStringForIndexes(),
            project = project,
            scope = scope
        ) { it.getClassId() == classId }
            .toList()

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        return listOfNotNull(getTypeAliasByClassId(classId)) //todo
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return fileBasedIndex.getNamesInPackage(KotlinTopLevelCallableByPackageShortNameIndex.NAME, packageFqName, scope)
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return fileBasedIndex.getNamesInPackage(KotlinTopLevelClassLikeDeclarationByPackageShortNameIndex.NAME, packageFqName, scope)
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        return KotlinFileFacadeClassByPackageIndex.get(stubIndex, packageFqName.asString(), project, scope)
    }

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false

    override fun computePackageNames(): Set<String>? = null
//        contextualModule?.let { IdeKotlinModulePackageNamesProvider.getInstance(project).computePackageNames(it) }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        //TODO original LC has platformSourcesFirst()
        return KotlinFileFacadeFqNameIndex.get(stubIndex, facadeFqName.asString(), project, scope)
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        return KotlinMultiFileClassPartIndex.get(stubIndex, facadeFqName.asString(), project, scope)
    }

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> {
        return KotlinScriptFqnIndex.get(stubIndex, scriptFqName.asString(), project, scope)
    }

    private fun getTypeAliasByClassId(classId: ClassId): KtTypeAlias? {
        return firstMatchingOrNull(
            stubKey = KotlinTopLevelTypeAliasFqNameIndex.indexKey,
            key = classId.asStringForIndexes(),
            filter = { candidate -> candidate.getClassId() == classId }
        ) ?: firstMatchingOrNull(stubKey = KotlinInnerTypeAliasClassIdIndex.indexKey, key = classId.asString())
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        KotlinTopLevelPropertyFqnNameIndex.get(stubIndex, callableId.asTopLevelStringForIndexes(), project, scope)

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        KotlinTopLevelFunctionFqnNameIndex.get(stubIndex, callableId.asTopLevelStringForIndexes(), project, scope)

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        val callableIdString = callableId.asTopLevelStringForIndexes()

        return buildSet {
            stubIndex.getContainingFilesIterator(
                indexId = KotlinTopLevelPropertyFqnNameIndex.indexKey,
                dataKey = callableIdString,
                project = project,
                scope = scope
            )
                .forEach { file ->
                    //check canceled is done inside findFile
                    psiManager.findFile(file)?.safeAs<KtFile>()?.let { add(it) }
                }
            stubIndex.getContainingFilesIterator(KotlinTopLevelFunctionFqnNameIndex.indexKey, callableIdString, project, scope)
                .forEach { file ->
                    //check canceled is done inside findFile
                    psiManager.findFile(file)?.safeAs<KtFile>()?.let { add(it) }
                }
        }
    }


    companion object {
        private val log = Logger.getInstance(IdeKotlinDeclarationProvider::class.java)

        private fun CallableId.asTopLevelStringForIndexes(): String {
            require(this.classId == null) {
                "Expecting top-level callable, but was $this"
            }

            if (packageName.isRoot) return callableName.asString()
            return "${packageName.asString()}.${callableName.asString()}"
        }

        private fun ClassId.asStringForIndexes(): String {
            if (packageFqName.isRoot) return relativeClassName.asString()
            return "${packageFqName.asString()}.${relativeClassName.asString()}"
        }
    }
}