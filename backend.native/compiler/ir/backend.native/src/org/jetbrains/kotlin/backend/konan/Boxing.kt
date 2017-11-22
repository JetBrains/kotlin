/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.types.KotlinType

internal fun KonanSymbols.getTypeConversion(
        actualType: KotlinType,
        expectedType: KotlinType
): IrSimpleFunctionSymbol? {
    val actualValueType = actualType.correspondingValueType
    val expectedValueType = expectedType.correspondingValueType

    return when {
        actualValueType == expectedValueType -> null

        actualValueType == null && expectedValueType != null -> {
            // This may happen in the following cases:
            // 1.  `actualType` is `Nothing`;
            // 2.  `actualType` is incompatible.

            this.getUnboxFunction(expectedValueType)
        }

        actualValueType != null && expectedValueType == null -> {
            this.boxFunctions[actualValueType]!!
        }

        else -> throw IllegalArgumentException("actual type is $actualType, expected $expectedType")
    }
}

internal fun KonanSymbols.getUnboxFunction(valueType: ValueType): IrSimpleFunctionSymbol =
        this.unboxFunctions[valueType]
                ?: this.boxClasses[valueType]!!.getPropertyGetter("value")!! as IrSimpleFunctionSymbol
