/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

abstract class PlatformSpecificOverridabilityRules : FirComposableSessionComponent<PlatformSpecificOverridabilityRules> {
    // Thus functions return "null" in case the status should be defined via standard platform-independent rules
    abstract fun isOverriddenFunction(
        overrideCandidate: FirNamedFunction,
        baseDeclaration: FirNamedFunction
    ): Boolean?

    abstract fun isOverriddenProperty(
        overrideCandidate: FirCallableDeclaration,
        baseDeclaration: FirProperty
    ): Boolean?

    abstract fun chooseIntersectionVisibility(
        overrides: Collection<FirCallableSymbol<*>>,
        dispatchClassSymbol: FirRegularClassSymbol?,
    ): Visibility?

    @SessionConfiguration
    final override fun createComposed(components: List<PlatformSpecificOverridabilityRules>): Composed {
        return Composed(components)
    }

    class Composed(
        override val components: List<PlatformSpecificOverridabilityRules>
    ) : PlatformSpecificOverridabilityRules(), FirComposableSessionComponent.Composed<PlatformSpecificOverridabilityRules> {
        override fun isOverriddenFunction(
            overrideCandidate: FirNamedFunction,
            baseDeclaration: FirNamedFunction,
        ): Boolean? {
            return components.firstNotNullOfOrNull { it.isOverriddenFunction(overrideCandidate, baseDeclaration) }
        }

        override fun isOverriddenProperty(
            overrideCandidate: FirCallableDeclaration,
            baseDeclaration: FirProperty,
        ): Boolean? {
            return components.firstNotNullOfOrNull { it.isOverriddenProperty(overrideCandidate, baseDeclaration) }
        }

        override fun chooseIntersectionVisibility(
            overrides: Collection<FirCallableSymbol<*>>,
            dispatchClassSymbol: FirRegularClassSymbol?,
        ): Visibility? {
            return components.firstNotNullOfOrNull { it.chooseIntersectionVisibility(overrides, dispatchClassSymbol) }
        }
    }
}

val FirSession.platformSpecificOverridabilityRules: PlatformSpecificOverridabilityRules? by FirSession.nullableSessionComponentAccessor()
