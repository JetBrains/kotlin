/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trasformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation

internal interface FirLazyTransformerForIDE {
    fun transformDeclaration()

    companion object {
        val EMPTY = object : FirLazyTransformerForIDE {
            override fun transformDeclaration() = Unit
        }

        fun FirDeclarationDesignation.ensurePhase(firResolvePhase: FirResolvePhase, exceptLast: Boolean) {
            val designationList = if (exceptLast) path else fullDesignation
            designationList.forEach { firDeclaration ->
                check (firDeclaration.resolvePhase >= firResolvePhase) {
                    "Designation element phase required to be $firResolvePhase but element resolved to ${firDeclaration.resolvePhase}"
                }
            }
        }
    }
}