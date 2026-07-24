/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.platform.TargetPlatform

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsSymbolRelationProvider {
    public fun containingSymbol(symbol: KaSymbol): KaSymbol?

    public fun containingDeclaration(symbol: KaSymbol): KaDeclarationSymbol?

    public fun containingFile(symbol: KaSymbol): KaFileSymbol?

    public fun containingModule(symbol: KaSymbol): KaModule

    public fun samConstructor(symbol: KaClassLikeSymbol): KaSamConstructorSymbol?

    public fun functionalInterfaceFunction(symbol: KaClassLikeSymbol): KaNamedFunctionSymbol?

    public fun functionalInterface(symbol: KaSamConstructorSymbol): KaClassLikeSymbol

    public fun functionalInterfaceFunction(symbol: KaSamConstructorSymbol): KaNamedFunctionSymbol

    public fun originalConstructorIfTypeAliased(symbol: KaConstructorSymbol): KaConstructorSymbol?

    public fun allOverriddenSymbols(symbol: KaCallableSymbol): Sequence<KaCallableSymbol>

    public fun directlyOverriddenSymbols(symbol: KaCallableSymbol): Sequence<KaCallableSymbol>

    public fun isSubClassOf(symbol: KaClassSymbol, superClass: KaClassSymbol): Boolean

    public fun isDirectSubClassOf(symbol: KaClassSymbol, superClass: KaClassSymbol): Boolean

    public fun intersectionOverriddenSymbols(symbol: KaCallableSymbol): List<KaCallableSymbol>

    @KaExperimentalApi
    public fun implementationState(symbol: KaCallableSymbol, implementerClassSymbol: KaClassSymbol): KaCallableImplementationState?

    public fun fakeOverrideOriginal(symbol: KaCallableSymbol): KaCallableSymbol

    public fun getExpectsForActual(symbol: KaDeclarationSymbol): List<KaDeclarationSymbol>

    public fun sealedClassInheritors(symbol: KaNamedClassSymbol): List<KaNamedClassSymbol>

    public fun hasConflictingSignatureWith(symbol: KaFunctionSymbol, other: KaFunctionSymbol, targetPlatform: TargetPlatform): Boolean
}
