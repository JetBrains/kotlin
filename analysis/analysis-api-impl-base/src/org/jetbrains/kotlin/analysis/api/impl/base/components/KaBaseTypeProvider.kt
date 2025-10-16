/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaTypeProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
abstract class KaBaseTypeProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaTypeProvider {
    override val KaValueParameterSymbol.varargArrayType: KaType?
        get() = withValidityAssertion {
            if (!isVararg) {
                return null
            }

            return analysisSession.buildVarargArrayType(returnType)
        }

    override val KaClassifierSymbol.defaultTypeWithStarProjections: KaType
        get() = withValidityAssertion {
            return if (this is KaClassLikeSymbol) {
                analysisSession.typeCreator.classType(this@defaultTypeWithStarProjections)
            } else {
                this.defaultType
            }
        }
}