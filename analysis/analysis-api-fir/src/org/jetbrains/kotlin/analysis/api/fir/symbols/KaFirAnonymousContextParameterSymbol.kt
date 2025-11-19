/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeTypePointer
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.contextParameterTypes
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal class KaFirAnonymousContextParameterSymbol(
    private val enclosingType: ConeClassLikeTypeImpl,
    private val index: Int,
    private val type: ConeKotlinType,
    private val builder: KaSymbolByFirBuilder,
) : KaContextParameterSymbol() {
    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaFirAnnotationListForType.create(type, builder) }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { Visibilities.Local }

    override val name: Name
        get() = withValidityAssertion { SpecialNames.NO_NAME_PROVIDED }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.SOURCE_MEMBER_GENERATED }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val returnType: KaType
        get() = withValidityAssertion { builder.typeBuilder.buildKtType(type) }

    override val token: KaLifetimeToken
        get() = builder.token

    override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol> = withValidityAssertion {
        val enclosingTypePointer = enclosingType.createPointer(builder)
        KaFirAnonymousContextParameterSymbolPointer(enclosingTypePointer, index)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KaFirAnonymousContextParameterSymbol) return false
        return index == other.index
                && enclosingType == other.enclosingType
                && type == other.type // Just in case
    }

    override fun hashCode(): Int {
        var result = index // Avoid auto-boxing by 'Objects.hashCode()'
        result = 31 * result + enclosingType.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

private class KaFirAnonymousContextParameterSymbolPointer(
    private val enclosingTypePointer: ConeTypePointer<ConeClassLikeTypeImpl>,
    private val index: Int
) : KaSymbolPointer<KaContextParameterSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaContextParameterSymbol? {
        requireIsInstance<KaFirSession>(analysisSession)

        val enclosingType = enclosingTypePointer.restore(analysisSession) ?: return null
        val type = enclosingType.contextParameterTypes(analysisSession.firSession).takeIf { it.size > index }?.get(index) ?: return null
        return KaFirAnonymousContextParameterSymbol(enclosingType, index, type, analysisSession.firSymbolBuilder)
    }
}