/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.Name

class FirClassUseSiteMemberScope(
    session: FirSession,
    superTypesScope: FirSuperTypeScope,
    declaredMemberScope: FirScope
) : AbstractFirUseSiteMemberScope(session, FirStandardOverrideChecker(session), superTypesScope, declaredMemberScope) {

    override fun processPropertiesByName(name: Name, processor: (FirCallableSymbol<*>) -> ProcessorAction): ProcessorAction {
        val seen = mutableSetOf<FirCallableSymbol<*>>()
        if (!declaredMemberScope.processPropertiesByName(name) {
                seen += it
                processor(it)
            }
        ) return STOP

        return superTypesScope.processPropertiesByName(name) {

            val overriddenBy = it.getOverridden(seen)
            if (overriddenBy == null) {
                processor(it)
            } else {
                NEXT
            }
        }
    }
}


