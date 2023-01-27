/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.platformStatusProvider
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirPlatformStatusResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.PLATFORM_STATUS) {
    override val transformer: FirTransformer<Nothing?> = FirPlatformStatusResolveTransformer(session)
}

class FirPlatformStatusResolveTransformer(
    override val session: FirSession,
) : FirAbstractTreeTransformer<Nothing?>(FirResolvePhase.PLATFORM_STATUS) {
    private val platformStatusProvider = session.platformStatusProvider

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        platformStatusProvider.withCalculatedStatusOf(file) {
            file.transformChildren(this, data)
        }

        return file
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): FirStatement {
        platformStatusProvider.calculateStatusFor(typeAlias)
        return typeAlias
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        platformStatusProvider.withCalculatedStatusOf(regularClass) {
            regularClass.transformChildren(this, data)
        }

        return regularClass
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: Nothing?): FirStatement {
        platformStatusProvider.calculateStatusFor(enumEntry)
        return enumEntry
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): FirStatement {
        platformStatusProvider.withCalculatedStatusOf(property) {
            property.getter?.let {
                transformFunction(it, data)
            }

            property.setter?.let {
                transformFunction(it, data)
            }
        }

        return property
    }

    override fun transformConstructor(constructor: FirConstructor, data: Nothing?): FirStatement {
        return transformFunction(constructor, data)
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): FirStatement {
        return transformFunction(simpleFunction, data)
    }

    override fun transformFunction(function: FirFunction, data: Nothing?): FirStatement {
        platformStatusProvider.calculateStatusFor(function)
        return function
    }
}
