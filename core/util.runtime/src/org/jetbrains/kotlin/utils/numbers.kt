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

package org.jetbrains.kotlin.utils

data class NumberWithRadix(val number: String, val radix: Int)

fun extractRadix(value: String): NumberWithRadix = when {
    value.startsWith("0x") || value.startsWith("0X") -> NumberWithRadix(value.substring(2), 16)
    value.startsWith("0b") || value.startsWith("0B") -> NumberWithRadix(value.substring(2), 2)
    else -> NumberWithRadix(value, 10)
}
