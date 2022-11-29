/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.utils.errors.requireWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId

object FirElementFinder {
    fun findClassifierWithClassId(firFile: FirFile, classId: ClassId): FirClassLikeDeclaration? {
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
        var result: FirClassLikeDeclaration? = null

        fun find(declarations: List<FirDeclaration>, classIdPathIndex: Int) {
            if (result != null) return
            val currentClassSegment = classIdPathSegment[classIdPathIndex]

            for (subDeclaration in declarations) {
                if (subDeclaration is FirClassLikeDeclaration && currentClassSegment == subDeclaration.symbol.name) {
                    if (classIdPathIndex == classIdPathSegment.lastIndex) {
                        result = subDeclaration
                        return
                    }
                    if (subDeclaration is FirRegularClass) {
                        find(subDeclaration.declarations, classIdPathIndex + 1)
                    }
                }
            }
        }

        find(firFile.declarations, classIdPathIndex = 0)
        return result
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

            override fun visitRegularClass(regularClass: FirRegularClass) {
                // Checking the rest super types that weren't resolved on the first OUTER_CLASS_ARGUMENTS_REQUIRED check in FirTypeResolver
                val oldResolvePhase = regularClass.resolvePhase
                val oldList = regularClass.superTypeRefs.toList()

                try {
                    super.visitRegularClass(regularClass)
                } catch (e: ConcurrentModificationException) {
                    val newResolvePhase = regularClass.resolvePhase
                    val newList = regularClass.superTypeRefs.toList()

                    throw IllegalStateException(
                        """
                        CME while traversing superTypeRefs of declaration=${regularClass.render()}:
                        classId: ${regularClass.classId},
                        oldPhase: $oldResolvePhase, oldList: ${oldList.joinToString(",", "[", "]") { it.render() }},
                        newPhase: $newResolvePhase, newList: ${newList.joinToString(",", "[", "]") { it.render() }}
                        """.trimIndent(), e
                    )
                }
            }
        })
        return result
    }
}