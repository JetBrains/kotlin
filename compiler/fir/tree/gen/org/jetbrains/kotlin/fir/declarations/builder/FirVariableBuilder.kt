/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.declarations.builder

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

@FirBuilderDsl
interface FirVariableBuilder : FirDeclarationBuilder {
    abstract override var source: KtSourceElement?
    abstract override var resolvePhase: FirResolvePhase
    abstract override val annotations: MutableList<FirAnnotation>
    abstract override var moduleData: FirModuleData
    abstract override var origin: FirDeclarationOrigin
    abstract override var attributes: FirDeclarationAttributes
    abstract var status: FirDeclarationStatus
    abstract var returnTypeRef: FirTypeRef
    abstract var receiverParameter: FirReceiverParameter?
    abstract var staticReceiverParameter: FirTypeRef?
    abstract var deprecationsProvider: DeprecationsProvider
    abstract var containerSource: DeserializedContainerSource?
    abstract var dispatchReceiverType: ConeSimpleKotlinType?
    abstract val contextParameters: MutableList<FirValueParameter>
    abstract var name: Name
    abstract var initializer: FirExpression?
    abstract var delegate: FirExpression?
    abstract var isVar: Boolean
    abstract var getter: FirPropertyAccessor?
    abstract var setter: FirPropertyAccessor?
    abstract var backingField: FirBackingField?
    override fun build(): FirVariable
}
