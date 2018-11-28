/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.impl.*
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment

abstract class AbstractFirResolveWithSessionTestCase : KotlinTestWithEnvironment() {

    open fun createSession(): FirSession {
        return object : FirSessionBase() {
            init {
                val firProvider = FirProviderImpl(this)
                registerComponent(FirProvider::class, firProvider)
                registerComponent(
                    FirSymbolProvider::class,
                    FirCompositeSymbolProvider(listOf(firProvider, JavaSymbolProvider(project), FirLibrarySymbolProviderImpl(this)))
                )
                registerComponent(FirQualifierResolver::class, FirQualifierResolverImpl(this))
                registerComponent(FirTypeResolver::class, FirTypeResolverImpl())
            }
        }
    }
}