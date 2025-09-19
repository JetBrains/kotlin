/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

sealed class FirDeclarationNameInvalidCharsProvider : FirComposableSessionComponent<FirDeclarationNameInvalidCharsProvider> {
    abstract val invalidChars: Set<Char>

    class Composed(
        override val components: List<FirDeclarationNameInvalidCharsProvider>,
    ) : FirDeclarationNameInvalidCharsProvider(), FirComposableSessionComponent.Composed<FirDeclarationNameInvalidCharsProvider> {
        override val invalidChars: Set<Char> = components.flatMapTo(mutableSetOf()) { it.invalidChars }
    }

    class Simple(override val invalidChars: Set<Char>) : FirDeclarationNameInvalidCharsProvider(),
        FirComposableSessionComponent.Single<FirDeclarationNameInvalidCharsProvider>

    @SessionConfiguration
    override fun createComposed(components: List<FirDeclarationNameInvalidCharsProvider>): Composed {
        return Composed(components)
    }

    companion object {
        fun of(invalidChars: Set<Char>): FirDeclarationNameInvalidCharsProvider = Simple(invalidChars)
    }
}

private val FirSession.declarationNameInvalidCharsProvider: FirDeclarationNameInvalidCharsProvider? by FirSession.nullableSessionComponentAccessor()

val FirSession.declarationNameInvalidChars: Set<Char> get() = declarationNameInvalidCharsProvider?.invalidChars ?: emptySet()
