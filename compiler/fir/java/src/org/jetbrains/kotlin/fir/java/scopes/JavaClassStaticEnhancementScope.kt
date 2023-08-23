/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.enhancement.FirSignatureEnhancement
import org.jetbrains.kotlin.fir.scopes.FirDelegatingContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class JavaClassStaticEnhancementScope(
    session: FirSession,
    owner: FirRegularClassSymbol,
    private val useSiteStaticScope: JavaClassStaticUseSiteScope,
) : FirDelegatingContainingNamesAwareScope(useSiteStaticScope) {
    private val signatureEnhancement = FirSignatureEnhancement(owner.fir, session) {
        emptyList()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        useSiteStaticScope.processPropertiesByName(name) process@{ original ->
            processor(signatureEnhancement.enhancedProperty(original, name))
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        useSiteStaticScope.processFunctionsByName(name) process@{ original ->
            val enhancedFunction = signatureEnhancement.enhancedFunction(original, name)
            if (enhancedFunction is FirNamedFunctionSymbol) {
                processor(enhancedFunction)
            }
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteStaticScope.processDeclaredConstructors process@{ original ->
            val function = signatureEnhancement.enhancedFunction(original, name = null)
            processor(function as FirConstructorSymbol)
        }
    }
}
