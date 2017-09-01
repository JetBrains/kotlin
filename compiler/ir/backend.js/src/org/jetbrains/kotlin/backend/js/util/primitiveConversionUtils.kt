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

package org.jetbrains.kotlin.backend.js.util

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral

fun JsBuilder.toByte(value: JsExpression) = "Kotlin".dotPure("toByte").invoke(value).pure()

fun JsBuilder.toChar(value: JsExpression) = "Kotlin".dotPure("toChar").invoke(value).pure()

fun JsBuilder.toShort(value: JsExpression) = "Kotlin".dotPure("toShort").invoke(value).pure()

fun JsBuilder.intToLong(value: JsExpression) = "Kotlin".dotPure("Long").dotPure("fromInt").invoke(value).pure()

fun JsBuilder.numberToLong(value: JsExpression) = "Kotlin".dotPure("Long").dotPure("fromNumber").invoke(value).pure()

fun JsBuilder.longToInt(value: JsExpression) = value.dotPure("toInt").invoke().pure()

fun JsBuilder.longToNumber(value: JsExpression) = value.dotPure("toNumber").invoke().pure()

fun JsBuilder.numberToInt(value: JsExpression) = value.bitOr(JsIntLiteral(0))