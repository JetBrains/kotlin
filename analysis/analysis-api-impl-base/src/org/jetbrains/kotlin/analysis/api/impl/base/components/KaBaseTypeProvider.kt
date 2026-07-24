/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
abstract class KaBaseTypeProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsTypeProvider {
    override fun varargArrayType(symbol: KaValueParameterSymbol): KaType? = withValidityAssertion {
        if (!symbol.isVararg) {
            return null
        }

        return analysisSession.buildVarargArrayType(symbol.returnType)
    }

    override fun defaultTypeWithStarProjections(symbol: KaClassifierSymbol): KaType = withValidityAssertion {
        return if (symbol is KaClassLikeSymbol) {
            analysisSession.typeCreator.classType(symbol)
        } else {
            defaultType(symbol)
        }
    }
}
