/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.findPsi
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
    /**
     * The underlying [FirBasedSymbol] which is used to provide other property implementations.
     */
    val firSymbol: S

    val analysisSession: KaFirSession
    val builder: KaSymbolByFirBuilder get() = analysisSession.firSymbolBuilder

    override val token: KaLifetimeToken get() = analysisSession.token
    override val origin: KaSymbolOrigin get() = withValidityAssertion { symbolOrigin() }
    override val psi: PsiElement? get() = withValidityAssertion { findPsi() }

    override val realPsi: PsiElement?
        get() = withValidityAssertion {
            backingPsi ?: when (origin) {
                KaSymbolOrigin.SOURCE,
                KaSymbolOrigin.LIBRARY,
                KaSymbolOrigin.JAVA_SOURCE,
                KaSymbolOrigin.JAVA_LIBRARY,
                    -> firSymbol.realPsi

                else -> null
            }
        }

    override val anchorPsi: PsiElement?
        get() = withValidityAssertion {
            realPsi ?: firSymbol.source?.psi
        }
}

private val FirBasedSymbol<*>.realPsi: PsiElement?
    get() {
        when (fir.origin) {
            FirDeclarationOrigin.Source,
            FirDeclarationOrigin.Library,
            is FirDeclarationOrigin.Java,
            FirDeclarationOrigin.BuiltIns,
            FirDeclarationOrigin.BuiltInsFallback,
            FirDeclarationOrigin.Precompiled,
                -> {
                // Recheck `FirDeclarationOrigin` as it's more specific than `KaSymbolOrigin`
                // In particular, `Enhancement` shouldn't have real psi as they cannot be restored
            }

            else -> return null
        }

        @OptIn(SuspiciousFakeSourceCheck::class)
        return when (val source = source) {
            is KtRealPsiSourceElement -> source.psi

            // Not all source elements are real. For instance, default property accessors are treated as sources,
            // but they have a fake source, which points to the containing declaration
            is KtFakePsiSourceElement -> when (source.kind) {
                KtFakeSourceElementKind.ReceiverFromType -> source.psi
                else -> null
            }

            else -> null
        }
    }

private val KaFirSymbol<*>.backingPsi: PsiElement?
    get() = (this as? KaFirPsiSymbol<*, *>)?.backingPsi

internal fun KaFirSymbol<*>.symbolEquals(other: Any?): Boolean = when {
    this === other -> true
    other == null || this::class != other::class -> false
    else -> this.firSymbol == (other as KaFirSymbol<*>).firSymbol
}

internal fun KaFirSymbol<*>.symbolOrigin(): KaSymbolOrigin = firSymbol.fir.ktSymbolOrigin()

internal fun KaFirSymbol<*>.symbolHashCode(): Int = firSymbol.hashCode()

internal tailrec fun FirDeclaration.ktSymbolOrigin(): KaSymbolOrigin = when (origin) {
    FirDeclarationOrigin.Source -> {
        when (source?.kind) {
            KtFakeSourceElementKind.ImplicitConstructor,
            is KtFakeSourceElementKind.DataClassGeneratedMembers, /* Valid for copy() / componentX(), should we change it? */
            is KtFakeSourceElementKind.EnumGeneratedDeclaration,
            KtFakeSourceElementKind.ItLambdaParameter,
                -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED

            else -> KaSymbolOrigin.SOURCE
        }
    }

    FirDeclarationOrigin.Precompiled -> KaSymbolOrigin.SOURCE
    FirDeclarationOrigin.Library, FirDeclarationOrigin.BuiltIns, FirDeclarationOrigin.BuiltInsFallback -> KaSymbolOrigin.LIBRARY
    is FirDeclarationOrigin.Java.Source -> KaSymbolOrigin.JAVA_SOURCE
    is FirDeclarationOrigin.Java.Library -> KaSymbolOrigin.JAVA_LIBRARY
    FirDeclarationOrigin.SamConstructor -> KaSymbolOrigin.SAM_CONSTRUCTOR
    FirDeclarationOrigin.Enhancement, FirDeclarationOrigin.RenamedForOverride -> when (source?.kind) {
        is KtFakeSourceElementKind.EnumGeneratedDeclaration -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED
        else -> javaOriginBasedOnSessionKind()
    }
    FirDeclarationOrigin.IntersectionOverride -> KaSymbolOrigin.INTERSECTION_OVERRIDE
    FirDeclarationOrigin.Delegated -> KaSymbolOrigin.DELEGATED
    FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk -> KaSymbolOrigin.LIBRARY
    FirDeclarationOrigin.Synthetic.TypeAliasConstructor -> KaSymbolOrigin.TYPEALIASED_CONSTRUCTOR
    is FirDeclarationOrigin.Synthetic -> {
        when {
            source?.kind is KtFakeSourceElementKind.DataClassGeneratedMembers -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED
            this is FirValueParameter && this.containingDeclarationSymbol.origin is FirDeclarationOrigin.Synthetic -> KaSymbolOrigin.SOURCE_MEMBER_GENERATED
            this is FirSyntheticProperty || this is FirSyntheticPropertyAccessor -> KaSymbolOrigin.JAVA_SYNTHETIC_PROPERTY
            origin is FirDeclarationOrigin.Synthetic.ForwardDeclaration -> KaSymbolOrigin.NATIVE_FORWARD_DECLARATION
            origin is FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer -> KaSymbolOrigin.SOURCE

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
        val original = (this as FirNamedFunction).originalForWrappedIntegerOperator?.fir
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
    is FirDeclarationOrigin.ForeignValue -> KaSymbolOrigin.SOURCE
    is FirDeclarationOrigin.FromOtherReplSnippet ->
        errorWithAttachment("Unsupported origin: ${origin::class.simpleName}") {
            withFirEntry("declaration", this@ktSymbolOrigin)
        }
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
        FirSession.Kind.Source -> KaSymbolOrigin.JAVA_SOURCE
        FirSession.Kind.Library -> KaSymbolOrigin.JAVA_LIBRARY
    }
}
