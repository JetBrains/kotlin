/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsScopeProvider {
    public fun memberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun staticMemberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun combinedMemberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun declaredMemberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun staticDeclaredMemberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun combinedDeclaredMemberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun delegatedMemberScope(symbol: KaDeclarationContainerSymbol): KaScope

    public fun fileScope(symbol: KaFileSymbol): KaScope

    public fun packageScope(symbol: KaPackageSymbol): KaScope

    public fun asCompositeScope(scopes: List<KaScope>): KaScope

    @KaExperimentalApi
    public fun scope(type: KaType): KaTypeScope?

    @KaExperimentalApi
    public fun declarationScope(typeScope: KaTypeScope): KaScope

    @KaExperimentalApi
    public fun syntheticJavaPropertiesScope(type: KaType): KaTypeScope?

    public fun scopeContext(file: KtFile, position: KtElement): KaScopeContext

    public fun importingScopeContext(file: KtFile): KaScopeContext

    public fun compositeScope(scopeContext: KaScopeContext, filter: (KaScopeKind) -> Boolean): KaScope
}
