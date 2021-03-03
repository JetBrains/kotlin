/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.declarations.FirDeclaration

class FirDesignation(val designation: List<FirDeclaration>) {
    private var currentIndex = 0

    fun canGoNext(): Boolean = currentIndex <= designation.lastIndex

    fun isTargetDeclaration(): Boolean = currentIndex == designation.lastIndex

    val currentDeclaration get() = designation[currentIndex]

    fun goNext() {
        if (canGoNext()) {
            currentIndex++
        } else {
            throw IndexOutOfBoundsException()
        }
    }
}