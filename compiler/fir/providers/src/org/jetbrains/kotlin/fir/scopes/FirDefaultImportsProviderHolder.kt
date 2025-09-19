/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.resolve.DefaultImportsProvider

sealed class FirDefaultImportsProviderHolder : FirComposableSessionComponent<FirDefaultImportsProviderHolder> {
    companion object {
        fun of(provider: DefaultImportsProvider): FirDefaultImportsProviderHolder {
            return Single(provider)
        }
    }

    class Single(override val provider: DefaultImportsProvider) : FirDefaultImportsProviderHolder(),
        FirComposableSessionComponent.Single<FirDefaultImportsProviderHolder>

    class Composed(
        override val components: List<FirDefaultImportsProviderHolder>
    ) : FirDefaultImportsProviderHolder(), FirComposableSessionComponent.Composed<FirDefaultImportsProviderHolder> {
        override val provider: DefaultImportsProvider = DefaultImportsProvider.Composed(components.map { it.provider })
    }

    abstract val provider: DefaultImportsProvider

    @SessionConfiguration
    override fun createComposed(components: List<FirDefaultImportsProviderHolder>): Composed {
        return Composed(components)
    }
}

private val FirSession.defaultImportsProviderHolder: FirDefaultImportsProviderHolder by FirSession.sessionComponentAccessor()
val FirSession.defaultImportsProvider: DefaultImportsProvider get() = defaultImportsProviderHolder.provider
