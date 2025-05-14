/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.CallableId

private val BACKING_FIELD_CALLABLE_ID = CallableId(BACKING_FIELD)

@OptIn(FirImplementationDetail::class)
class FirDefaultPropertyBackingField(
    moduleData: FirModuleData,
    origin: FirDeclarationOrigin,
    source: KtSourceElement?,
    annotations: MutableList<FirAnnotation>,
    returnTypeRef: FirTypeRef,
    isVar: Boolean,
    propertySymbol: FirPropertySymbol,
    status: FirDeclarationStatus,
    resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR,
) : FirBackingFieldImpl(
    source = source,
    moduleData = moduleData,
    resolvePhase = resolvePhase,
    origin = origin,
    attributes = FirDeclarationAttributes(),
    returnTypeRef = returnTypeRef,
    staticReceiverParameter = null,
    deprecationsProvider = UnresolvedDeprecationProvider,
    name = BACKING_FIELD,
    isVar = isVar,
    isVal = !isVar,
    symbol = FirBackingFieldSymbol(BACKING_FIELD_CALLABLE_ID),
    propertySymbol = propertySymbol,
    initializer = null,
    annotations = annotations.toMutableOrEmpty(),
    status = status,
)

