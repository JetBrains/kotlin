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

package org.jetbrains.kotlin.backend.js.context

import org.jetbrains.kotlin.backend.js.intrinsics.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.backend.ast.JsScope
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.resolve.BindingTrace

class IrTranslationConfig(
        val module: IrModuleFragment,
        val bindingTrace: BindingTrace,
        val jsConfig: JsConfig,
        val scope: JsScope
) {
    private val intrinsicList = listOf(
            IntArithmeticIntrinsic, IntIncDecIntrinsic, IntUnaryArithmeticIntrinsic,
            LongArithmeticIntrinsic,
            SpecializedNumberConversionIntrinsic, NumberConversionIntrinsic,
            StringPlusIntrinsic,
            NotIntrinsic,
            CompareToIntrinsic,
            PrimitiveRangeToIntrinsic,
            EqualsIntrinsic, CompareZeroIntrinsic, IrNotIntrinsic, ReferenceEqualsIntrinsic,
            ThrowIntrinsic,
            FunctionInvokeIntrinsic,
            ArrayAccessIntrinsic, ArraySizeIntrinsic, ArrayOfNullsIntrinsic
    )
    private val intrinsicCache = mutableMapOf<FunctionDescriptor, IntrinsicHolder>()

    val intrinsics = object : Provider<FunctionDescriptor, FunctionIntrinsic?> {
        override fun get(key: FunctionDescriptor): FunctionIntrinsic? = intrinsicCache.getOrPut(key) {
            IntrinsicHolder(intrinsicList.firstOrNull { it.isApplicable(key) })
        }.intrinsic
    }

    inner class IntrinsicHolder(val intrinsic: FunctionIntrinsic?)

    val isTypedArraysEnabled: Boolean
        get() = jsConfig.configuration[JSConfigurationKeys.TYPED_ARRAYS_ENABLED] == true
}