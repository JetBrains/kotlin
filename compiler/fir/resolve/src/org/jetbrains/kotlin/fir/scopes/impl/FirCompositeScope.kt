/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.scopes.FirIterableScope
import org.jetbrains.kotlin.fir.scopes.FirScope

class FirCompositeScope(
    private val scopeList: MutableList<FirScope>,
    private val reversedPriority: Boolean = false
) : FirIterableScope() {
    override val scopes get() = if (reversedPriority) scopeList.asReversed() else scopeList

    fun addScopes(scopes: List<FirScope>) {
        scopeList += scopes
    }

    fun addScope(scope: FirScope) {
        scopeList += scope
    }

    fun dropLastScopes(number: Int) {
        repeat(number) {
            scopeList.removeAt(scopeList.size - 1)
        }
    }

}
