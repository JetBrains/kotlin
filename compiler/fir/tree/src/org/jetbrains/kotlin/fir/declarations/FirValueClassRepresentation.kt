/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

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

@OptIn(ValueClassBackendAgnosticApi::class)
fun FirRegularClassSymbol.inlineClassRepresentationInJvm(): InlineClassRepresentation<ConeRigidType>? =
    valueClassRepresentation?.interpretAsInlineClassRepresentationOrNull(
        treatCompatibleFullValueClassesAsInline = false,
        hasSuperClass = { error("No super class check must be called for inline classes in JVM") }
    )

val FirRegularClassSymbol.isFullValueClass: Boolean
    get() = valueClassRepresentation is FullValueClassRepresentation

@SymbolInternals
val FirRegularClass.isFullValueClass: Boolean
    get() = valueClassRepresentation is FullValueClassRepresentation

@SymbolInternals
val FirRegularClass.isInlineClass: Boolean
    get() = valueClassRepresentation is InlineClassRepresentation

val FirRegularClassSymbol.isInlineClass: Boolean
    get() = valueClassRepresentation is InlineClassRepresentation
