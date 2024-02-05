/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.patchDesignationPathIfNeeded
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.ifEmpty

internal object FirElementFinder {
    fun findClassifierWithClassId(
        firFile: FirFile,
        classId: ClassId,
    ): FirClassLikeDeclaration? = collectDesignationPath(
        firFile = firFile,
        containerClassId = classId.outerClassId,
        expectedDeclarationAcceptor = { it is FirClassLikeDeclaration && it.symbol.name == classId.shortClassName },
    )?.target?.let { it as FirClassLikeDeclaration }

    fun collectDesignationPath(
        firFile: FirFile,
        declarationContainerClassId: ClassId?,
        targetMemberDeclaration: FirDeclaration,
    ): FirDesignation? = collectDesignationPath(
        firFile = firFile,
        containerClassId = declarationContainerClassId,
        expectedDeclarationAcceptor = { it == targetMemberDeclaration },
    )

    fun findDeclaration(firFile: FirFile, nonLocalDeclaration: KtDeclaration): FirDeclaration? = collectDesignationPath(
        firFile = firFile,
        nonLocalDeclaration = nonLocalDeclaration,
    )?.declarationTarget

    fun findPathToDeclarationWithTarget(
        firFile: FirFile,
        nonLocalDeclaration: KtDeclaration,
    ): List<FirDeclaration>? = collectDesignationPath(
        firFile = firFile,
        nonLocalDeclaration = nonLocalDeclaration,
    )?.let { it.path + it.declarationTarget }

    fun collectDesignationPath(
        firFile: FirFile,
        nonLocalDeclaration: KtDeclaration,
    ): FirDesignation? = collectDesignationPath(
        firFile = firFile,
        containerClassId = nonLocalDeclaration.containingClassOrObject?.getClassId(),
        expectedDeclarationAcceptor = { it.psi == nonLocalDeclaration },
    )

    /**
     * @see collectDesignationPath
     */
    private val FirDesignation.declarationTarget: FirDeclaration get() = target as FirDeclaration

    /**
     * @return [FirDesignation] where [FirDesignation.target] is [FirDeclaration]
     *
     * @see declarationTarget
     */
    private fun collectDesignationPath(
        firFile: FirFile,
        containerClassId: ClassId?,
        expectedDeclarationAcceptor: (FirDeclaration) -> Boolean,
    ): FirDesignation? {
        if (containerClassId != null) {
            requireWithAttachment(!containerClassId.isLocal, { "ClassId should not be local" }) {
                withEntry("classId", containerClassId) { it.asString() }
            }

            requireWithAttachment(
                firFile.packageFqName == containerClassId.packageFqName,
                { "ClassId should not be local" }
            ) {
                withEntry("FirFile.packageName", firFile.packageFqName) { it.asString() }
                withEntry("ClassId.packageName", containerClassId.packageFqName) { it.asString() }
            }
        }

        val classIdPathSegment = containerClassId?.relativeClassName?.pathSegments().orEmpty()
        val path = ArrayList<FirDeclaration>(classIdPathSegment.size + 2)
        var result: FirDeclaration? = null

        fun find(declarations: Iterable<FirDeclaration>, classIdPathIndex: Int): Boolean {
            val currentClassSegment = classIdPathSegment.getOrNull(classIdPathIndex)
            for (subDeclaration in declarations) {
                when {
                    currentClassSegment == null && expectedDeclarationAcceptor(subDeclaration) -> {
                        result = subDeclaration
                        return true
                    }

                    subDeclaration is FirScript -> {
                        path += subDeclaration
                        val scriptDeclarations = subDeclaration.declarations
                        if (find(scriptDeclarations, classIdPathIndex)) {
                            return true
                        }

                        path.removeLast()
                        continue
                    }

                    subDeclaration is FirCodeFragment -> {
                        val codeFragmentDeclarations = subDeclaration.block.statements.asSequence().filterIsInstance<FirDeclaration>()
                        if (find(codeFragmentDeclarations.asIterable(), classIdPathIndex)) {
                            return true
                        }

                        continue
                    }

                    subDeclaration is FirRegularClass && currentClassSegment == subDeclaration.symbol.name -> {
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

        path += firFile
        find(firFile.declarations, classIdPathIndex = 0)

        if (result == null) {
            return null
        }

        // K1 doesn't perform smart-casts on 'result'
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        return FirDesignation(
            path = patchDesignationPathIfNeeded(result!!, path).ifEmpty { emptyList() },
            target = result!!,
        )
    }

    inline fun <reified E : FirElement> findElementIn(
        container: FirElement,
        crossinline canGoInside: (E) -> Boolean = { true },
        crossinline predicate: (E) -> Boolean,
    ): E? {
        var result: E? = null
        container.accept(object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) {
                when {
                    result != null -> return
                    element !is E || element is FirFile -> element.acceptChildren(this)
                    predicate(element) -> result = element
                    canGoInside(element) -> element.acceptChildren(this)
                }
            }
        })

        return result
    }
}