/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.BasicValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.JvmInlineMultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.descriptors.toInlineRepresentation
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeRigidType

private object FirValueClassRepresentationKey : FirDeclarationDataKey()

var FirRegularClass.valueClassRepresentation: ValueClassRepresentation<ConeRigidType>?
        by FirDeclarationDataRegistry.data(FirValueClassRepresentationKey)

fun FirRegularClass.inlineClassRepresentation(distinguishBasicAndFull: Boolean): InlineClassRepresentation<ConeRigidType>? =
    valueClassRepresentation?.toInlineRepresentation(distinguishBasicAndFull = distinguishBasicAndFull)

val FirRegularClass.jvmInlineMultiFieldValueClassRepresentation: JvmInlineMultiFieldValueClassRepresentation<ConeRigidType>?
    get() = valueClassRepresentation as? JvmInlineMultiFieldValueClassRepresentation<ConeRigidType>

val FirRegularClass.isFullValueClass: Boolean
    get() = valueClassRepresentation is FullValueClassRepresentation

val FirRegularClass.isBasicValueClass: Boolean
    get() = valueClassRepresentation is BasicValueClassRepresentation

val FirRegularClassSymbol.isFullValueClass: Boolean
    get() {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        return fir.valueClassRepresentation is FullValueClassRepresentation
    }

val FirRegularClassSymbol.isBasicValueClass: Boolean
    get() {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        return fir.valueClassRepresentation is BasicValueClassRepresentation
    }
