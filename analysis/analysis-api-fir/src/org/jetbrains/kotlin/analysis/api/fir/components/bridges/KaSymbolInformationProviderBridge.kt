/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.components.KaDeprecation
import org.jetbrains.kotlin.analysis.api.components.KaDeprecationLevel
import org.jetbrains.kotlin.analysis.api.components.KaReturnValueStatus
import org.jetbrains.kotlin.analysis.api.components.KaSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.analysis.api.symbols.KaDeprecation as KaEndpointDeprecation
import org.jetbrains.kotlin.analysis.api.symbols.KaDeprecationLevel as KaEndpointDeprecationLevel
import org.jetbrains.kotlin.analysis.api.symbols.KaReturnValueStatus as KaEndpointReturnValueStatus
import org.jetbrains.kotlin.analysis.api.symbols.applicableAnnotationTargets as applicableAnnotationTargetsEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.canBeOperator as canBeOperatorEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.containingFileAnnotations as containingFileAnnotationsEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.defaultAnnotationTargets as defaultAnnotationTargetsEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.deprecation as deprecationEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.importableFqName as importableFqNameEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.isDeprecated as isDeprecatedEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.isInline as isInlineEndpoint
import org.jetbrains.kotlin.analysis.api.symbols.returnValueStatus as returnValueStatusEndpoint

/**
 * Routes the legacy [KaSymbolInformationProvider] surface through the new public `context(session: KaSession)` endpoints in
 * `org.jetbrains.kotlin.analysis.api.symbols`, which in turn reach the
 * [org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolInformationProvider] proxy.
 */
internal class KaSymbolInformationProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaSymbolInformationProvider, KaFirSessionComponent {
    override val KaSymbol.deprecation: KaDeprecation?
        get() = context(analysisSession) { deprecationEndpoint?.toLegacyDeprecation() }

    override val KaSymbol.isDeprecated: Boolean
        get() = context(analysisSession) { isDeprecatedEndpoint }

    override val KaNamedFunctionSymbol.canBeOperator: Boolean
        get() = context(analysisSession) { canBeOperatorEndpoint }

    override val KaClassSymbol.applicableAnnotationTargets: Set<KaAnnotationTarget>?
        get() = context(analysisSession) { applicableAnnotationTargetsEndpoint }

    override val KaKotlinPropertySymbol.isInline: Boolean
        get() = context(analysisSession) { isInlineEndpoint }

    override val KaSymbol.importableFqName: FqName?
        get() = context(analysisSession) { importableFqNameEndpoint }

    override val KaSymbol.defaultAnnotationTargets: Set<KaAnnotationTarget>?
        get() = context(analysisSession) { defaultAnnotationTargetsEndpoint }

    override val KaNamedFunctionSymbol.returnValueStatus: KaReturnValueStatus
        get() = context(analysisSession) { returnValueStatusEndpoint.toLegacyReturnValueStatus() }

    override val KaDeclarationSymbol.containingFileAnnotations: KaAnnotationList?
        get() = context(analysisSession) { containingFileAnnotationsEndpoint }
}

private fun KaEndpointDeprecation.toLegacyDeprecation(): KaDeprecation = object : KaDeprecation {
    override val level: KaDeprecationLevel = this@toLegacyDeprecation.level.toLegacyLevel()
    override val isPropagatedToOverrides: Boolean = this@toLegacyDeprecation.isPropagatedToOverrides

    override fun toString(): String = "KaDeprecation(level=$level, isPropagatedToOverrides=$isPropagatedToOverrides)"
}

private fun KaEndpointDeprecationLevel.toLegacyLevel(): KaDeprecationLevel = when (this) {
    KaEndpointDeprecationLevel.WARNING -> KaDeprecationLevel.WARNING
    KaEndpointDeprecationLevel.ERROR -> KaDeprecationLevel.ERROR
    KaEndpointDeprecationLevel.HIDDEN -> KaDeprecationLevel.HIDDEN
    else -> error("Unexpected deprecation level: $this")
}

private fun KaEndpointReturnValueStatus.toLegacyReturnValueStatus(): KaReturnValueStatus = when (this) {
    KaEndpointReturnValueStatus.MustUse -> KaReturnValueStatus.MustUse
    KaEndpointReturnValueStatus.ExplicitlyIgnorable -> KaReturnValueStatus.ExplicitlyIgnorable
    KaEndpointReturnValueStatus.Unspecified -> KaReturnValueStatus.Unspecified
    else -> error("Unexpected return value status: $this")
}
