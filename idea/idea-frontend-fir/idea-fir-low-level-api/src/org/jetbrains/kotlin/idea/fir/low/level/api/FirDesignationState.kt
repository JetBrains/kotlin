/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile

abstract class ContextByDesignationCollector<C : Any>(private val designation: FirDeclarationDesignation) {
    private var context: C? = null
    private val designationState = FirDesignationState(designation)

    protected abstract fun getCurrentContext(): C
    protected abstract fun goToNestedDeclaration(declaration: FirDeclaration)

    fun getCollectedContext(): C {
        return context
            ?: error("Context is not collected yet")
    }

    fun nextStep() {
        if (designationState.canGoNext()) {
            designationState.goNext()
            if (designationState.currentDeclarationIfPresent == designation.declaration) {
                check(context == null)
                context = getCurrentContext()
            }
            goToNestedDeclaration(designationState.currentDeclaration)
        } else {
            if (designationState.currentDeclarationIfPresent == designation.declaration) {
                designationState.goToInnerDeclaration()
            }
        }
    }
}

private class FirDesignationState(val designation: FirDeclarationDesignation) {
    /**
     * Holds current declaration index
     * if `currentIndex in [0, designation.path.lastIndex]` then current declaration is in path
     * if `currentIndex == `designation.path.lastIndex + 1` then current declaration is our target declaration
     * if `currentIndex > designation.path.lastIndex + 1` then we are inside current declaration
     */
    private var currentIndex = -1

    fun canGoNext(): Boolean = currentIndex < designation.path.size

    val currentDeclarationIfPresent: FirDeclaration?
        get() = when (currentIndex) {
            in designation.path.indices -> designation.path[currentIndex]
            designation.path.size -> designation.declaration
            else -> null
        }

    val currentDeclaration: FirDeclaration
        get() = currentDeclarationIfPresent
            ?: error("Went inside target declaration")

    fun goNext() {
        if (canGoNext()) {
            currentIndex++
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    fun goToInnerDeclaration() {
        if (currentIndex == designation.path.size) {
            currentIndex++
        } else {
            throw IndexOutOfBoundsException()
        }
    }
}