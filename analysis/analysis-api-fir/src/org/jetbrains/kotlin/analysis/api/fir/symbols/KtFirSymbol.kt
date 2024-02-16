/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.scopes.impl.originalForWrappedIntegerOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

internal interface KtFirSymbol<out S : FirBasedSymbol<*>> : KtSymbol, KtLifetimeOwner {
    val firSymbol: S
    val analysisSession: KtFirAnalysisSession
    val builder: KtSymbolByFirBuilder get() = analysisSession.firSymbolBuilder

    override val token: KtLifetimeToken get() = analysisSession.token
    override val origin: KtSymbolOrigin get() = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }
}

internal fun KtFirSymbol<*>.symbolEquals(other: Any?): Boolean {
    if (other !is KtFirSymbol<*>) return false
    return this.firSymbol == other.firSymbol
}

internal fun KtFirSymbol<*>.symbolHashCode(): Int = firSymbol.hashCode()

internal tailrec fun FirDeclaration.ktSymbolOrigin(): KtSymbolOrigin = when (origin) {
    FirDeclarationOrigin.Source -> {
        when (source?.kind) {
            KtFakeSourceElementKind.ImplicitConstructor,
            KtFakeSourceElementKind.DataClassGeneratedMembers, /* Valid for copy() / componentX(), should we change it? */
            KtFakeSourceElementKind.EnumGeneratedDeclaration,
            KtFakeSourceElementKind.ItLambdaParameter -> KtSymbolOrigin.SOURCE_MEMBER_GENERATED

            else -> KtSymbolOrigin.SOURCE
        }
    }

    FirDeclarationOrigin.Precompiled -> KtSymbolOrigin.SOURCE
    FirDeclarationOrigin.Library, FirDeclarationOrigin.BuiltIns -> KtSymbolOrigin.LIBRARY
    is FirDeclarationOrigin.Java -> KtSymbolOrigin.JAVA
    FirDeclarationOrigin.SamConstructor -> KtSymbolOrigin.SAM_CONSTRUCTOR
    FirDeclarationOrigin.Enhancement -> KtSymbolOrigin.JAVA
    FirDeclarationOrigin.IntersectionOverride -> KtSymbolOrigin.INTERSECTION_OVERRIDE
    FirDeclarationOrigin.Delegated -> KtSymbolOrigin.DELEGATED
    FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk -> KtSymbolOrigin.LIBRARY
    is FirDeclarationOrigin.Synthetic -> {
        when {
            source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers -> KtSymbolOrigin.SOURCE_MEMBER_GENERATED
            this is FirValueParameter && this.containingFunctionSymbol.origin is FirDeclarationOrigin.Synthetic -> KtSymbolOrigin.SOURCE_MEMBER_GENERATED
            this is FirSyntheticProperty || this is FirSyntheticPropertyAccessor -> KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY

            else -> errorWithAttachment("Invalid FirDeclarationOrigin ${origin::class.simpleName}") {
                withFirEntry("firToGetOrigin", this@ktSymbolOrigin)
            }
        }
    }

    FirDeclarationOrigin.ImportedFromObjectOrStatic -> {
        val importedFromObjectData = (this as FirCallableDeclaration).importedFromObjectOrStaticData
            ?: errorWithAttachment("Declaration has ImportedFromObject origin, but no importedFromObjectData present") {
                withFirEntry("firToGetOrigin", this@ktSymbolOrigin)
            }

        importedFromObjectData.original.ktSymbolOrigin()
    }

    FirDeclarationOrigin.WrappedIntegerOperator -> {
        val original = (this as FirSimpleFunction).originalForWrappedIntegerOperator?.fir
            ?: errorWithFirSpecificEntries(
                "Declaration has WrappedIntegerOperator origin, but no originalForWrappedIntegerOperator present",
                fir = this
            )

        original.ktSymbolOrigin()
    }

    is FirDeclarationOrigin.Plugin -> KtSymbolOrigin.PLUGIN
    FirDeclarationOrigin.RenamedForOverride -> KtSymbolOrigin.JAVA
    is FirDeclarationOrigin.SubstitutionOverride -> KtSymbolOrigin.SUBSTITUTION_OVERRIDE
    FirDeclarationOrigin.DynamicScope -> KtSymbolOrigin.JS_DYNAMIC
    is FirDeclarationOrigin.ScriptCustomization -> KtSymbolOrigin.PLUGIN
}

internal fun KtClassLikeSymbol.getSymbolKind(): KtSymbolKind {
    val firSymbol = firSymbol
    return when {
        firSymbol.classId.isNestedClass -> KtSymbolKind.CLASS_MEMBER
        firSymbol.isLocal -> KtSymbolKind.LOCAL
        else -> KtSymbolKind.TOP_LEVEL
    }
}

