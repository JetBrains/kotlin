/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.collectForEach
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.yieldIfNotNull

internal abstract class FileBasedKotlinDeclarationProvider : KotlinDeclarationProvider() {
    abstract val ktFiles: List<KtFile>

    open fun mightContainPackage(packageFqName: FqName): Boolean = true

    private val KtFile.topLevelDeclarations: Sequence<KtDeclaration>
        get() {
            return sequence {
                for (child in declarations) {
                    if (child is KtScript) {
                        yieldAll(child.declarations)
                    } else {
                        yield(child)
                    }
                }
            }
        }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        if (!mightContainPackage(classId.packageFqName)) return null
        return ktFiles.firstNotNullOfOrNull { getClassLikeDeclarationsByClassId(it, classId).firstOrNull() }
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        if (!mightContainPackage(classId.packageFqName)) return emptyList()
        return collectForEach(mutableListOf()) {
            getClassLikeDeclarationsByClassId(it, classId).filterIsInstanceTo<KtClassOrObject, _>(this)
        }
    }

    private fun getClassLikeDeclarationsByClassId(kotlinFile: KtFile, classId: ClassId): Sequence<KtClassLikeDeclaration> {
        if (classId.isLocal) {
            return emptySequence()
        }

        if (kotlinFile.packageFqName != classId.packageFqName) {
            return emptySequence()
        }

        data class Task(val chunks: List<Name>, val element: KtElement)

        return sequence {
            val tasks = ArrayDeque<Task>()

            val startingChunks = classId.relativeClassName.pathSegments()
            for (declaration in kotlinFile.topLevelDeclarations) {
                tasks.addLast(Task(startingChunks, declaration))
            }

            tasks += Task(startingChunks, kotlinFile)

            while (!tasks.isEmpty()) {
                val (chunks, element) = tasks.removeFirst()
                assert(chunks.isNotEmpty())

                if (element !is KtNamedDeclaration || element.nameAsName != chunks[0]) {
                    continue
                }

                if (chunks.size == 1) {
                    yieldIfNotNull(element as? KtClassLikeDeclaration)
                    continue
                }

                if (element is KtDeclarationContainer) {
                    val newChunks = chunks.subList(1, chunks.size)
                    for (child in element.declarations) {
                        tasks.addLast(Task(newChunks, child))
                    }
                }
            }
        }
    }

    override fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> {
        if (!mightContainPackage(classId.packageFqName)) return emptyList()
        return collectForEach(mutableListOf()) {
            getClassLikeDeclarationsByClassId(it, classId).filterIsInstanceTo<KtTypeAlias, _>(this)
        }
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        if (!mightContainPackage(packageFqName)) return emptySet()
        return collectForEach(mutableSetOf()) {
            getTopLevelDeclarationNamesTo<KtClassLikeDeclaration>(this, it, packageFqName)
        }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        if (!mightContainPackage(callableId.packageName)) return emptyList()
        return collectForEach(mutableSetOf()) { getTopLevelCallablesTo(this, it, callableId) }
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        if (!mightContainPackage(callableId.packageName)) return emptyList()
        return collectForEach(mutableSetOf()) { getTopLevelCallablesTo(this, it, callableId) }
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        if (!mightContainPackage(callableId.packageName)) return emptyList()
        return buildSet {
            getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
            getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
        }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        if (!mightContainPackage(packageFqName)) return emptySet()
        return collectForEach(mutableSetOf()) {
            getTopLevelDeclarationNamesTo<KtCallableDeclaration>(this, it, packageFqName)
        }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        if (!mightContainPackage(packageFqName)) return emptyList()
        return ktFiles.filter { ktFile ->
            ktFile.packageFqName == packageFqName
        }
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (!mightContainPackage(facadeFqName.parent())) return emptyList()
        return ktFiles.filter { ktFile ->
            ktFile.javaFileFacadeFqName == facadeFqName && ktFile.isFacade
        }
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> = emptyList()

    private inline fun <reified T : KtCallableDeclaration> getTopLevelCallablesTo(
        to: MutableCollection<T>,
        kotlinFile: KtFile,
        callableId: CallableId,
    ) {
        require(callableId.classId == null)
        getTopLevelDeclarationsTo(to, kotlinFile, callableId.packageName, callableId.callableName)
    }

    private inline fun <reified T : KtNamedDeclaration> getTopLevelDeclarationsTo(
        to: MutableCollection<T>,
        kotlinFile: KtFile,
        packageFqName: FqName,
        name: Name
    ) {
        if (kotlinFile.packageFqName != packageFqName) {
            return
        }

        for (declaration in kotlinFile.topLevelDeclarations) {
            if (declaration is T && declaration.nameAsName == name) {
                to.add(declaration)
            }
        }
    }

    private inline fun <reified T : KtNamedDeclaration> getTopLevelDeclarationNamesTo(
        to: MutableSet<Name>,
        kotlinFile: KtFile,
        packageFqName: FqName
    ) {
        if (kotlinFile.packageFqName != packageFqName) {
            return
        }

        for (declaration in kotlinFile.topLevelDeclarations) {
            if (declaration is T) {
                to.addIfNotNull(declaration.nameAsName)
            }
        }
    }

    private inline fun <E, C : MutableCollection<E>> collectForEach(
        collection: C,
        collect: C.(KtFile) -> Unit
    ): C {
        return ktFiles.collectForEach(collection, collect)
    }

    private val KtFile.isFacade: Boolean
        get() = hasTopLevelCallables()
}

internal class NonLazyFileBasedKotlinDeclarationProvider(
    override val ktFiles: List<KtFile>
) : FileBasedKotlinDeclarationProvider() {
    constructor(ktFile: KtFile) : this(listOf(ktFile))
}