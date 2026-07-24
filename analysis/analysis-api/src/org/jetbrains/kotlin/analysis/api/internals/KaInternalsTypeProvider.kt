/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaBuiltinTypes
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsTypeProvider {
    public fun builtinTypes(): KaBuiltinTypes

    public fun approximateToDenotableSupertype(type: KaType, allowLocalDenotableTypes: Boolean): KaType?

    public fun approximateToDenotableSubtype(type: KaType): KaType?

    public fun approximateToDenotableSupertype(type: KaType, position: KtElement): KaType?

    public fun augmentedByWarningLevelAnnotations(type: KaType): KaType

    public fun defaultType(symbol: KaClassifierSymbol): KaType

    public fun defaultTypeWithStarProjections(symbol: KaClassifierSymbol): KaType

    public fun varargArrayType(symbol: KaValueParameterSymbol): KaType?

    public fun commonSupertype(types: Iterable<KaType>): KaType

    public fun type(typeReference: KtTypeReference): KaType

    public fun receiverType(expression: KtDoubleColonExpression): KaType?

    public fun withNullability(type: KaType, isMarkedNullable: Boolean): KaType

    public fun hasCommonSubtypeWith(type: KaType, that: KaType): Boolean

    public fun collectImplicitReceiverTypes(position: KtElement): List<KaType>

    public fun directSupertypes(type: KaType, shouldApproximate: Boolean): Sequence<KaType>

    public fun allSupertypes(type: KaType, shouldApproximate: Boolean): Sequence<KaType>

    public fun arrayElementType(type: KaType): KaType?
}
