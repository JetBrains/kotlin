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

package org.jetbrains.kotlin.incremental.web

data class TranslationResultValue(val metadata: ByteArray, val binaryAst: ByteArray, val inlineData: ByteArray)


data class IrTranslationResultValue(
    val fileData: ByteArray,
    val types: ByteArray,
    val signatures: ByteArray,
    val strings: ByteArray,
    val declarations: ByteArray,
    val bodies: ByteArray,
    val fqn: ByteArray,
    val debugInfo: ByteArray?
)
