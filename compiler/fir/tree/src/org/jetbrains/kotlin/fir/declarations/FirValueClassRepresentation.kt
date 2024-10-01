/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.MultiFieldValueClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.fir.types.ConeDenotableType

private object FirValueClassRepresentationKey : FirDeclarationDataKey()

var FirRegularClass.valueClassRepresentation: ValueClassRepresentation<ConeDenotableType>?
        by FirDeclarationDataRegistry.data(FirValueClassRepresentationKey)

val FirRegularClass.inlineClassRepresentation: InlineClassRepresentation<ConeDenotableType>?
    get() = valueClassRepresentation as? InlineClassRepresentation<ConeDenotableType>

val FirRegularClass.multiFieldValueClassRepresentation: MultiFieldValueClassRepresentation<ConeDenotableType>?
    get() = valueClassRepresentation as? MultiFieldValueClassRepresentation<ConeDenotableType>


