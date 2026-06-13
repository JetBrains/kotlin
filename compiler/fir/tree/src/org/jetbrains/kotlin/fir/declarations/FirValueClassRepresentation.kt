/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.BasicValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.JvmInlineMultiFieldValueClassRepresentation
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
 * Determines whether the current [FirRegularClassSymbol] is compatible with being a single-field value class.
 *
 * The compatibility is assessed based on the type of value class representation associated with the [FirRegularClassSymbol].
 *
 * **Full** value classes are value classes described in [this KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0454-better-immutability-value-classes-MFVC.md).
 *
 * **Basic** value classes are [inline classes](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0104-inline-classes.md) and [jvm inline multi-field value classes](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0340-multi-field-value-classes.md)
 *
 * The overview of full value classes is that they are value classes without @JvmInline annotation on all backends, supporting one or multiple underlying fields.
 *
 * They are not optimized on JVM, regardless of the number of underlying fields. On other backends, they are optimized if there is only one underlying field.
 *
 * @param treatFullValueClassesWithOneFieldAsBasic A boolean indicating whether to treat full value classes with one underlying field as basic (inline class).
 *                                                 On JVM full value classes are not unboxed on the behalf of Kotlin compiler while `inline class`es/`@JvmInline value class`es are.
 *                                                 On other platforms there is no `@JvmInline` annotation and unboxing is done by the compiler in both basic and full value classes with a single field.
 *                                                 Therefore, full value classes with one field are actually preexisting value classes on other platforms.
 *                                                 `false` must be used for JVM, `true` for other backends.
 * @return An [InlineClassRepresentation] if the class has a compatible value class
 *         representation and meets the conditions specified by the `treatFullValueClassesWithOneFieldAsBasic`
 *         parameter; otherwise, `null`.
 */
@ValueClassBackendAgnosticApi
fun FirRegularClassSymbol.inlineClassRepresentation(treatFullValueClassesWithOneFieldAsBasic: Boolean): InlineClassRepresentation<ConeRigidType>? =
    valueClassRepresentation?.interpretAsInlineClassRepresentationOrNull(treatFullValueClassesWithOneFieldAsBasic)

val FirRegularClassSymbol.jvmInlineMultiFieldValueClassRepresentation: JvmInlineMultiFieldValueClassRepresentation<ConeRigidType>?
    get() = valueClassRepresentation as? JvmInlineMultiFieldValueClassRepresentation<ConeRigidType>

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
