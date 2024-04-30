/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

class DesignationState private constructor(
    val firstDeclaration: FirDeclaration,
    private val designation: Iterator<FirDeclaration>,
    val targetClass: FirClassLikeDeclaration
) {
    companion object {
        fun create(
            symbol: FirRegularClassSymbol,
            designationMapForLocalClasses: Map<FirClassLikeDeclaration, FirClassLikeDeclaration?>,
            includeFile: Boolean
        ): DesignationState? {
            val regularClass = symbol.fir
            val designation = if (regularClass.isLocal) buildList {
                var klass: FirClassLikeDeclaration = regularClass
                while (true) {
                    this.add(klass)
                    klass = designationMapForLocalClasses[klass]?.takeIf { it !is FirAnonymousObject } ?: break
                }
                reverse()
            } else buildList<FirDeclaration> {
                val firProvider = regularClass.moduleData.session.firProvider
                val outerClasses = generateSequence(symbol.classId) { classId ->
                    classId.outerClassId
                }.mapTo(mutableListOf()) { firProvider.getFirClassifierByFqName(it) }
                val file = firProvider.getFirClassifierContainerFileIfAny(regularClass.symbol)
                requireNotNull(file) { "Containing file was not found for\n${regularClass.render()}" }
                if (includeFile) {
                    this += file
                }
                this += outerClasses.filterNotNull().asReversed()
            }
            if (designation.isEmpty()) return null
            return DesignationState(designation.first(), designation.iterator(), regularClass)
        }
    }

    private var currentElement: FirDeclaration? = null
    var classLocated: Boolean = false
        private set

    fun shouldSkipClass(declaration: FirDeclaration): Boolean {
        if (classLocated) return declaration != targetClass
        if (currentElement == null && designation.hasNext()) {
            currentElement = designation.next()
        }
        val result = currentElement == declaration
        if (result) {
            if (currentElement == targetClass) {
                classLocated = true
            }
            currentElement = null
        }
        return !result
    }
}
