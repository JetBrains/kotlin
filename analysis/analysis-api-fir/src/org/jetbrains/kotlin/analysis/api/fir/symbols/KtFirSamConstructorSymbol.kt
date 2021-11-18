/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firRef
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirSamConstructorSymbol(
    fir: FirSimpleFunction,
    resolveState: FirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtSamConstructorSymbol(), KtFirSymbol<FirSimpleFunction> {
    override val firRef = firRef(fir, resolveState)
    override val psi: PsiElement? by firRef.withFirAndCache { fir -> fir.findPsi(fir.moduleData.session) }
    override val name: Name get() = firRef.withFir { it.name }
    override val type: KtType by cached {
        firRef.returnType(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, builder)
    }

    override val valueParameters: List<KtValueParameterSymbol> by firRef.withFirAndCache { fir ->
        fir.valueParameters.map { valueParameter ->
            builder.variableLikeBuilder.buildValueParameterSymbol(valueParameter)
        }
    }

    override val hasStableParameterNames: Boolean = firRef.withFir { it.getHasStableParameterNames(it.moduleData.session) }

    override val isExtension: Boolean get() = firRef.withFir { it.receiverTypeRef != null }
    override val receiverType: KtType? by cached {
        firRef.receiverType(builder)
    }

    override val callableIdIfNonLocal: CallableId? get() = getCallableIdIfNonLocal()

    override fun createPointer(): KtSymbolPointer<KtSamConstructorSymbol> {
        return firRef.withFir { fir ->
            val callableId = fir.symbol.callableId
            KtFirSamConstructorSymbolPointer(ClassId(callableId.packageName, callableId.callableName))
        }
    }
}
