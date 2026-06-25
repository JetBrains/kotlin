/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.components.KaScopeProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsScopeProvider
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.analysis.api.scopes.asCompositeScope as asCompositeScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope as combinedDeclaredMemberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.combinedMemberScope as combinedMemberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.declarationScope as declarationScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope as declaredMemberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.delegatedMemberScope as delegatedMemberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.fileScope as fileScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.memberScope as memberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.packageScope as packageScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.scope as scopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.staticDeclaredMemberScope as staticDeclaredMemberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.staticMemberScope as staticMemberScopeEndpoint
import org.jetbrains.kotlin.analysis.api.scopes.syntheticJavaPropertiesScope as syntheticJavaPropertiesScopeEndpoint

/**
 * Routes the legacy [KaScopeProvider] surface through the new public `context(session: KaSession)` scope endpoints, which in turn reach the
 * [KaInternalsScopeProvider] proxy. Members without a public endpoint ([KaScopeProvider.scopeContext],
 * [KaScopeProvider.importingScopeContext], and [KaScopeProvider.compositeScope], whose shape is still under design) are forwarded straight
 * to the proxy.
 */
internal class KaScopeProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaScopeProvider {
    private val proxy: KaInternalsScopeProvider
        get() = analysisSession.scopeProvider

    override val KaDeclarationContainerSymbol.memberScope: KaScope
        get() = context(analysisSession) { memberScopeEndpoint }

    override val KaDeclarationContainerSymbol.staticMemberScope: KaScope
        get() = context(analysisSession) { staticMemberScopeEndpoint }

    override val KaDeclarationContainerSymbol.combinedMemberScope: KaScope
        get() = context(analysisSession) { combinedMemberScopeEndpoint }

    override val KaDeclarationContainerSymbol.declaredMemberScope: KaScope
        get() = context(analysisSession) { declaredMemberScopeEndpoint }

    override val KaDeclarationContainerSymbol.staticDeclaredMemberScope: KaScope
        get() = context(analysisSession) { staticDeclaredMemberScopeEndpoint }

    override val KaDeclarationContainerSymbol.combinedDeclaredMemberScope: KaScope
        get() = context(analysisSession) { combinedDeclaredMemberScopeEndpoint }

    override val KaDeclarationContainerSymbol.delegatedMemberScope: KaScope
        get() = context(analysisSession) { delegatedMemberScopeEndpoint }

    override val KaFileSymbol.fileScope: KaScope
        get() = context(analysisSession) { fileScopeEndpoint }

    override val KaPackageSymbol.packageScope: KaScope
        get() = context(analysisSession) { packageScopeEndpoint }

    override fun List<KaScope>.asCompositeScope(): KaScope =
        context(analysisSession) { asCompositeScopeEndpoint() }

    override val KaType.scope: KaTypeScope?
        get() = context(analysisSession) { scopeEndpoint }

    override val KaTypeScope.declarationScope: KaScope
        get() = context(analysisSession) { declarationScopeEndpoint }

    override val KaType.syntheticJavaPropertiesScope: KaTypeScope?
        get() = context(analysisSession) { syntheticJavaPropertiesScopeEndpoint }

    override fun KtFile.scopeContext(position: KtElement): KaScopeContext =
        proxy.scopeContext(this, position)

    override val KtFile.importingScopeContext: KaScopeContext
        get() = proxy.importingScopeContext(this)

    override fun KaScopeContext.compositeScope(filter: (KaScopeKind) -> Boolean): KaScope =
        proxy.compositeScope(this, filter)
}
