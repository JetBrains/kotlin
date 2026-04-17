/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.BasicValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ExtendedValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.descriptors.toInlineRepresentation
import org.jetbrains.kotlin.fir.types.ConeRigidType

private object FirValueClassRepresentationKey : FirDeclarationDataKey()

var FirRegularClass.valueClassRepresentation: ValueClassRepresentation<ConeRigidType>?
        by FirDeclarationDataRegistry.data(FirValueClassRepresentationKey)

fun FirRegularClass.inlineClassRepresentation(distinguishBasicAndExtended: Boolean): InlineClassRepresentation<ConeRigidType>? =
    valueClassRepresentation?.toInlineRepresentation(distinguishBasicAndExtended = distinguishBasicAndExtended)

val FirRegularClass.multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<ConeRigidType>?
    get() = valueClassRepresentation as? MultiFieldValueClassRepresentation<ConeRigidType>

val FirRegularClass.isExtendedValueClass: Boolean
    get() = valueClassRepresentation is ExtendedValueClassRepresentation

val FirRegularClass.isBasicValueClass: Boolean
    get() = valueClassRepresentation is BasicValueClassRepresentation
