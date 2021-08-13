/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.NAME_FOR_BACKING_FIELD
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.CallableId

@OptIn(FirImplementationDetail::class)
class FirDefaultPropertyBackingField(
    moduleData: FirModuleData,
    annotations: MutableList<FirAnnotationCall>,
    returnTypeRef: FirTypeRef,
    isVar: Boolean,
    propertySymbol: FirPropertySymbol,
    status: FirDeclarationStatus,
) : FirBackingFieldImpl(
    source = null,
    moduleData = moduleData,
    resolvePhase = FirResolvePhase.BODY_RESOLVE,
    origin = FirDeclarationOrigin.Synthetic,
    attributes = FirDeclarationAttributes(),
    returnTypeRef = returnTypeRef,
    receiverTypeRef = null,
    deprecation = null,
    containerSource = null,
    dispatchReceiverType = null,
    name = NAME_FOR_BACKING_FIELD,
    delegate = null,
    isVar = isVar,
    isVal = !isVar,
    getter = null,
    setter = null,
    backingField = null,
    symbol = FirBackingFieldSymbol(CallableId(NAME_FOR_BACKING_FIELD)),
    propertySymbol = propertySymbol,
    initializer = null,
    annotations = annotations,
    typeParameters = mutableListOf(),
    status = status,
)

