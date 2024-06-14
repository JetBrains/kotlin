/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.scopes.impl.importedFromObjectOrStaticData
import org.jetbrains.kotlin.fir.scopes.impl.originalForWrappedIntegerOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal interface KaFirSymbol<out S : FirBasedSymbol<*>> : KaSymbol, KaLifetimeOwner {
    val firSymbol: S
    val analysisSession: KaFirSession
    val builder: KaSymbolByFirBuilder get() = analysisSession.firSymbolBuilder

    override val token: KaLifetimeToken get() = analysisSession.token
    override val origin: KaSymbolOrigin get() = withValidityAssertion { firSymbol.fir.ktSymbolOrigin() }
}

internal fun KaFirSymbol<*>.symbolEquals(other: Any?): Boolean {
    if (other !is KaFirSymbol<*>) return false
    return this.firSymbol == other.firSymbol
}

internal fun KaFirSymbol<*>.symbolHashCode(): Int = firSymbol.hashCode()

internal tailrec fun FirDeclaration.ktSymbolOrigin(): KaSymbolOrigin = when (origin) {
    FirDeclarationOrigin.Source -> {
        when (source?.kind) {
            KtFakeSourceElementKind.ImplicitConstructor,
            KtFakeSourceElementKind.DataClassGeneratedMembers, /* Valid for copy() / componentX(), should we change it? */
            KtFakeSourceElementKind.EnumGeneratedDeclaration,
            KtFakeSourceElementKind.ItLambdaParameter -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED

            else -> KaSymbolOrigin.SOURCE
        }
    }

    FirDeclarationOrigin.Precompiled, FirDeclarationOrigin.CommonArtefact -> KaSymbolOrigin.SOURCE
    FirDeclarationOrigin.Library, FirDeclarationOrigin.BuiltIns -> KaSymbolOrigin.LIBRARY
    is FirDeclarationOrigin.Java.Source -> KaSymbolOrigin.JAVA_SOURCE
    is FirDeclarationOrigin.Java.Library -> KaSymbolOrigin.JAVA_LIBRARY
    FirDeclarationOrigin.SamConstructor -> KaSymbolOrigin.SAM_CONSTRUCTOR
    FirDeclarationOrigin.Enhancement, FirDeclarationOrigin.RenamedForOverride -> javaOriginBasedOnSessionKind()
    FirDeclarationOrigin.IntersectionOverride -> KaSymbolOrigin.INTERSECTION_OVERRIDE
    FirDeclarationOrigin.Delegated -> KaSymbolOrigin.DELEGATED
    FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk -> KaSymbolOrigin.LIBRARY
    is FirDeclarationOrigin.Synthetic -> {
        when {
            source?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED
            this is FirValueParameter && this.containingFunctionSymbol.origin is FirDeclarationOrigin.Synthetic -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED
            this is FirSyntheticProperty || this is FirSyntheticPropertyAccessor -> KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY
            origin is FirDeclarationOrigin.Synthetic.ForwardDeclaration -> KaSymbolOrigin.NATIVE_FORWARD_DECLARATION

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

    is FirDeclarationOrigin.Plugin -> KaSymbolOrigin.PLUGIN
    is FirDeclarationOrigin.SubstitutionOverride -> KaSymbolOrigin.SUBSTITUTION_OVERRIDE
    FirDeclarationOrigin.DynamicScope -> KaSymbolOrigin.JS_DYNAMIC
    is FirDeclarationOrigin.ScriptCustomization -> KaSymbolOrigin.PLUGIN
}

internal fun KaClassLikeSymbol.getSymbolKind(): KaSymbolLocation {
    val firSymbol = firSymbol
    return when {
        firSymbol.classId.isNestedClass -> KaSymbolLocation.CLASS
        firSymbol.isLocal -> KaSymbolLocation.LOCAL
        else -> KaSymbolLocation.TOP_LEVEL
    }
}

private fun FirDeclaration.javaOriginBasedOnSessionKind(): KaSymbolOrigin {
    return when (moduleData.session.kind) {
        FirSession.Kind.Source, FirSession.Kind.SeparateCompilationUnit -> KaSymbolOrigin.JAVA_SOURCE
        FirSession.Kind.Library -> KaSymbolOrigin.JAVA_LIBRARY
    }
}
