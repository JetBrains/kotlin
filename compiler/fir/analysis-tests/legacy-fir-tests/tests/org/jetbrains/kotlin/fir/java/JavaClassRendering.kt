/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaConstructor
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

fun renderJavaClass(renderer: FirRenderer, javaClass: FirJavaClass, session: FirSession) {
    val memberScope = javaClass.unsubstitutedScope(session, ScopeSession(), withForcedTypeCalculator = true)

    val staticScope = javaClass.scopeProvider.getStaticScope(javaClass, session, ScopeSession())

    renderer.renderAnnotations(javaClass)
    renderer.visitMemberDeclaration(javaClass)
    renderer.renderSupertypes(javaClass)
    renderer.renderInBraces {
        val renderedDeclarations = mutableListOf<FirDeclaration>()

        fun renderAndCache(symbol: FirCallableSymbol<*>) {
            val enhanced = symbol.fir
            if (enhanced !in renderedDeclarations) {
                enhanced.accept(renderer, null)
                renderer.newLine()
                renderedDeclarations += enhanced
            }
        }

        for (declaration in javaClass.declarations) {
            if (declaration in renderedDeclarations) continue

            val scopeToUse =
                if (declaration is FirCallableDeclaration && declaration.status.isStatic)
                    staticScope
                else
                    memberScope

            when (declaration) {
                is FirJavaConstructor -> scopeToUse!!.processDeclaredConstructors(::renderAndCache)
                is FirJavaMethod -> scopeToUse!!.processFunctionsByName(declaration.name, ::renderAndCache)
                is FirJavaField -> scopeToUse!!.processPropertiesByName(declaration.name, ::renderAndCache)
                is FirEnumEntry -> scopeToUse!!.processPropertiesByName(declaration.name, ::renderAndCache)
                else -> {
                    declaration.accept(renderer, null)
                    renderer.newLine()
                    renderedDeclarations += declaration
                }
            }
        }
    }
}
