/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.Name

class FirClassUseSiteScope(
    session: FirSession,
    private val superTypesScope: FirScope,
    private val declaredMemberScope: FirScope
) : AbstractFirOverrideScope(session) {

    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction): ProcessorAction {
        val seen = mutableSetOf<ConeCallableSymbol>()
        if (!declaredMemberScope.processFunctionsByName(name) {
                seen += it
                processor(it)
            }
        ) return STOP

        return superTypesScope.processFunctionsByName(name) {

            val overriddenBy = it.isOverridden(seen)
            if (overriddenBy == null) {
                processor(it)
            } else {
                NEXT
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction): ProcessorAction {
        val seen = mutableSetOf<ConeCallableSymbol>()
        if (!declaredMemberScope.processPropertiesByName(name) {
                seen += it
                processor(it)
            }
        ) return STOP

        return superTypesScope.processPropertiesByName(name) {

            val overriddenBy = it.isOverridden(seen)
            if (overriddenBy == null) {
                processor(it)
            } else {
                NEXT
            }
        }
    }

    override fun processClassifiersByName(name: Name, position: FirPosition, processor: (ConeClassifierSymbol) -> Boolean): Boolean {
        return declaredMemberScope.processClassifiersByName(name, position, processor)
    }
}


