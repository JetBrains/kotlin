/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.BasicValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassBackendAgnosticApi
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.descriptors.interpretAsInlineClassRepresentationOrNull
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.ConeRigidType

private object FirValueClassRepresentationKey : FirDeclarationDataKey()

var FirRegularClass.valueClassRepresentation: ValueClassRepresentation<ConeRigidType>?
        by FirDeclarationDataRegistry.data(FirValueClassRepresentationKey)

val FirRegularClassSymbol.valueClassRepresentation: ValueClassRepresentation<ConeRigidType>?
    get() {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        return fir.valueClassRepresentation
    }

/**
 * Retrieves an [InlineClassRepresentation] of the [FirRegularClassSymbol] if the class is an inline class or
 * computes an [InlineClassRepresentation] if [treatCompatibleFullValueClassesAsInline] is `true` and
 * the class is a compatible full value class.
 *
 * See [ValueClassRepresentation] documentation for more details about value class types and their compatibility.
 *
 * @return An [InlineClassRepresentation] or `null`.
 */
@ValueClassBackendAgnosticApi
fun FirRegularClassSymbol.inlineClassRepresentation(treatCompatibleFullValueClassesAsInline: Boolean): InlineClassRepresentation<ConeRigidType>? =
    valueClassRepresentation?.interpretAsInlineClassRepresentationOrNull(treatCompatibleFullValueClassesAsInline)

val FirRegularClassSymbol.isFullValueClass: Boolean
    get() = valueClassRepresentation is FullValueClassRepresentation

@SymbolInternals
val FirRegularClass.isFullValueClass: Boolean
    get() = valueClassRepresentation is FullValueClassRepresentation

@SymbolInternals
val FirRegularClass.isBasicValueClass: Boolean
    get() = valueClassRepresentation is BasicValueClassRepresentation

val FirRegularClassSymbol.isBasicValueClass: Boolean
    get() = valueClassRepresentation is BasicValueClassRepresentation
