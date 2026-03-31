/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.yieldIfNotNull

@KaPlatformInterface
public class KotlinFileBasedDeclarationProvider(public val kotlinFile: KtFile) : KotlinDeclarationProvider {
    private val topLevelDeclarations: Sequence<KtDeclaration>
        get() {
            return sequence {
                for (child in kotlinFile.declarations) {
                    @OptIn(KtExperimentalApi::class)
                    if (child is KtScript && !child.isReplSnippet) {
                        yieldAll(child.declarations)
                    } else {
                        yield(child)
                    }
                }
            }
        }

    override fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration? {
        return getDeclarationsByClassId(classId).filterIsInstance<KtClassLikeDeclaration>().firstOrNull()
    }

    override fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        return getDeclarationsByClassId(classId).filterIsInstance<KtClassOrObject>().toList()
    }

    private fun getDeclarationsByClassId(classId: ClassId): Sequence<KtNamedDeclaration> {
        @OptIn(ClassIdBasedLocality::class)
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
            for (declaration in topLevelDeclarations) {
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
                    yieldIfNotNull(element)
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
        return getDeclarationsByClassId(classId).filterIsInstance<KtTypeAlias>().toList()
    }

    override fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames(packageFqName) { it is KtClassLikeDeclaration || it is KtScript }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        return getTopLevelCallables(callableId)
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        return getTopLevelCallables(callableId)
    }

    override fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile> {
        return buildSet {
            getTopLevelProperties(callableId).mapTo(this) { it.containingKtFile }
            getTopLevelFunctions(callableId).mapTo(this) { it.containingKtFile }
        }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        return getTopLevelDeclarationNames(packageFqName) { it is KtCallableDeclaration }
    }

    override fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile> {
        if (kotlinFile.packageFqName != packageFqName) {
            return emptyList()
        }

        return listOf(kotlinFile)
    }

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (kotlinFile.javaFileFacadeFqName != facadeFqName) return emptyList()

        for (declaration in topLevelDeclarations) {
            if (declaration !is KtClassLikeDeclaration) {
                return listOf(kotlinFile)
            }
        }

        return emptyList()
    }

    override fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile> = emptyList()

    override fun computePackageNames(): Set<String> = setOf(kotlinFile.packageFqName.asString())

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false

    override fun findFilesForScript(scriptFqName: FqName): Collection<KtScript> =
        listOfNotNull(kotlinFile.script?.takeIf { it.fqName == scriptFqName })

    private inline fun <reified T : KtCallableDeclaration> getTopLevelCallables(callableId: CallableId): Collection<T> {
        require(callableId.classId == null)
        return getTopLevelDeclarations(callableId.packageName, callableId.callableName)
    }

    private inline fun <reified T : KtNamedDeclaration> getTopLevelDeclarations(packageFqName: FqName, name: Name): Collection<T> {
        if (kotlinFile.packageFqName != packageFqName) {
            return emptyList()
        }

        return buildList {
            for (declaration in topLevelDeclarations) {
                if (declaration is T && declaration.nameAsName == name) {
                    add(declaration)
                }
            }
        }
    }

    private inline fun getTopLevelDeclarationNames(packageFqName: FqName, filter: (KtNamedDeclaration) -> Boolean): Set<Name> {
        if (kotlinFile.packageFqName != packageFqName) {
            return emptySet()
        }

        return buildSet {
            for (declaration in topLevelDeclarations) {
                if (declaration is KtNamedDeclaration && filter(declaration)) {
                    addIfNotNull(declaration.nameAsName)
                }
            }
        }
    }
}
