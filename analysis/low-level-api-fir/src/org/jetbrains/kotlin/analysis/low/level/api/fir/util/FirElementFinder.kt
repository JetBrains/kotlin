/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal object FirElementFinder {
    fun findClassifierWithClassId(
        firFile: FirFile,
        classId: ClassId,
    ): FirClassLikeDeclaration? = collectDesignationPath(
        firFile = firFile,
        containerClassId = classId.outerClassId,
        expectedDeclarationAcceptor = { it is FirClassLikeDeclaration && it.symbol.name == classId.shortClassName },
    )?.target?.let { it as FirClassLikeDeclaration }

    fun findClassPathToDeclaration(
        firFile: FirFile,
        declarationContainerClassId: ClassId,
        targetMemberDeclaration: FirDeclaration,
    ): List<FirRegularClass>? = collectDesignationPath(
        firFile = firFile,
        containerClassId = declarationContainerClassId,
        expectedDeclarationAcceptor = { it == targetMemberDeclaration },
    )?.path

    fun findDeclaration(firFile: FirFile, nonLocalDeclaration: KtDeclaration): FirDeclaration? = collectDesignationPath(
        firFile = firFile,
        nonLocalDeclaration = nonLocalDeclaration,
    )?.target

    fun findPathToDeclarationWithTarget(
        firFile: FirFile,
        nonLocalDeclaration: KtDeclaration,
    ): List<FirDeclaration>? = collectDesignationPath(
        firFile = firFile,
        nonLocalDeclaration = nonLocalDeclaration,
    )?.pathWithTarget

    class FirDeclarationDesignation(
        val path: List<FirRegularClass>,
        val target: FirDeclaration,
    ) {
        val pathWithTarget: List<FirDeclaration> get() = path + target
    }

    fun collectDesignationPath(
        firFile: FirFile,
        nonLocalDeclaration: KtDeclaration,
    ): FirDeclarationDesignation? = collectDesignationPath(
        firFile = firFile,
        containerClassId = nonLocalDeclaration.containingClassOrObject?.getClassId(),
        expectedDeclarationAcceptor = { it.psi == nonLocalDeclaration },
    )

    private fun collectDesignationPath(
        firFile: FirFile,
        containerClassId: ClassId?,
        expectedDeclarationAcceptor: (FirDeclaration) -> Boolean,
    ): FirDeclarationDesignation? {
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
        val path = ArrayList<FirRegularClass>(classIdPathSegment.size)
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
                        val scriptDeclarations = subDeclaration.statements.asSequence().filterIsInstance<FirDeclaration>()
                        if (find(scriptDeclarations.asIterable(), classIdPathIndex)) {
                            return true
                        }

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

        find(firFile.declarations, classIdPathIndex = 0)
        return result?.let {
            FirDeclarationDesignation(
                path = if (path.isEmpty()) emptyList() else path,
                target = it,
            )
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