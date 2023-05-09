/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.utils.errors.requireWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId

object FirElementFinder {
    fun findClassifierWithClassId(
        firFile: FirFile,
        classId: ClassId,
    ): FirClassLikeDeclaration? = findPathToClassifierWithClassId(
        firFile = firFile,
        classId = classId,
        expectedMemberDeclaration = null
    )?.second

    fun findClassPathToDeclaration(
        firFile: FirFile,
        declarationContainerClassId: ClassId,
        targetMemberDeclaration: FirDeclaration,
    ): List<FirRegularClass>? = findPathToClassifierWithClassId(
        firFile = firFile,
        classId = declarationContainerClassId,
        expectedMemberDeclaration = targetMemberDeclaration
    )?.first

    private fun findPathToClassifierWithClassId(
        firFile: FirFile,
        classId: ClassId,
        expectedMemberDeclaration: FirDeclaration?,
    ): Pair<List<FirRegularClass>, FirClassLikeDeclaration>? {
        requireWithAttachmentBuilder(!classId.isLocal, { "ClassId should not be local" }) {
            withEntry("classId", classId) { it.asString() }
        }
        requireWithAttachmentBuilder(
            firFile.packageFqName == classId.packageFqName,
            { "ClassId should not be local" }
        ) {
            withEntry("FirFile.packageName", firFile.packageFqName) { it.asString() }
            withEntry("ClassId.packageName", classId.packageFqName) { it.asString() }
        }

        val classIdPathSegment = classId.relativeClassName.pathSegments()
        val path = ArrayList<FirRegularClass>(classIdPathSegment.size)
        var result: FirClassLikeDeclaration? = null

        fun find(declarations: Iterable<FirDeclaration>, classIdPathIndex: Int): Boolean {
            val currentClassSegment = classIdPathSegment[classIdPathIndex]

            for (subDeclaration in declarations) {
                if (subDeclaration is FirScript) {
                    val scriptDeclarations = subDeclaration.statements.asSequence().filterIsInstance<FirDeclaration>()
                    if (find(scriptDeclarations.asIterable(), classIdPathIndex)) {
                        return true
                    }

                    continue
                }

                if (subDeclaration is FirClassLikeDeclaration && currentClassSegment == subDeclaration.symbol.name) {
                    if (classIdPathIndex == classIdPathSegment.lastIndex) {
                        if (expectedMemberDeclaration == null ||
                            subDeclaration is FirRegularClass && expectedMemberDeclaration in subDeclaration.declarations
                        ) {
                            if (subDeclaration is FirRegularClass) {
                                path += subDeclaration
                            }

                            result = subDeclaration
                            return true
                        }

                        continue
                    }

                    if (subDeclaration is FirRegularClass) {
                        path += subDeclaration
                        if (find(subDeclaration.declarations, classIdPathIndex + 1)) {
                            return true
                        }

                        path.removeLast()
                    }
                }
            }

            return false
        }

        find(firFile.declarations, classIdPathIndex = 0)
        return result?.let {
            (path as List<FirRegularClass>) to it
        }
    }

    inline fun <reified E : FirElement> findElementIn(
        container: FirElement,
        crossinline canGoInside: (E) -> Boolean = { true },
        crossinline predicate: (E) -> Boolean,
    ): E? {
        var result: E? = null
        container.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (result != null) return
                when {
                    element !is E || element is FirFile -> {
                        element.acceptChildren(this)
                    }
                    predicate(element) -> {
                        result = element
                    }
                    canGoInside(element) -> {
                        element.acceptChildren(this)
                    }
                }
            }

            @OptIn(ResolveStateAccess::class)
            override fun visitRegularClass(regularClass: FirRegularClass) {
                // Checking the rest super types that weren't resolved on the first OUTER_CLASS_ARGUMENTS_REQUIRED check in FirTypeResolver
                val oldResolveState = regularClass.resolveState
                val oldList = regularClass.superTypeRefs.toList()

                try {
                    super.visitRegularClass(regularClass)
                } catch (e: ConcurrentModificationException) {
                    val newResolveState = regularClass.resolveState
                    val newList = regularClass.superTypeRefs.toList()

                    throw IllegalStateException(
                        """
                        CME while traversing superTypeRefs of declaration=${regularClass.render()}:
                        classId: ${regularClass.classId},
                        oldState: $oldResolveState, oldList: ${oldList.joinToString(",", "[", "]") { it.render() }},
                        newState: $newResolveState, newList: ${newList.joinToString(",", "[", "]") { it.render() }}
                        """.trimIndent(), e
                    )
                }
            }
        })
        return result
    }
}