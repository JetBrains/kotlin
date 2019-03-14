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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.Type

data class TypedKotlinValue(val type: Type, val value: KotlinExpression)
data class TypedNativeValue(val type: Type, val value: NativeExpression)

/**
 * Generates bridges between Kotlin and native, passing arbitrary native-typed values.
 *
 * It does the same as [SimpleBridgeGenerator] except that it supports any native types, e.g. struct values.
 */
interface MappingBridgeGenerator {
    fun kotlinToNative(
            builder: KotlinCodeBuilder,
            nativeBacked: NativeBacked,
            returnType: Type,
            kotlinValues: List<TypedKotlinValue>,
            independent: Boolean,
            block: NativeCodeBuilder.(nativeValues: List<NativeExpression>) -> NativeExpression
    ): KotlinExpression

    fun nativeToKotlin(
            builder: NativeCodeBuilder,
            nativeBacked: NativeBacked,
            returnType: Type,
            nativeValues: List<TypedNativeValue>,
            block: KotlinCodeBuilder.(kotlinValues: List<KotlinExpression>) -> KotlinExpression
    ): NativeExpression
}
