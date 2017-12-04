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

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.*

class TempFiles(val outputName: String) {
    val nativeBinaryFile    by lazy { File("${outputName}.kt.bc") }
    val cAdapterHeader      by lazy { File("${outputName}_api.h") }
    val cAdapterCpp         by lazy { createTempFile("api", ".cpp").deleteOnExit() }
    val cAdapterBitcode     by lazy { createTempFile("api", ".bc").deleteOnExit() }

    val nativeBinaryFileName    get() = nativeBinaryFile.absolutePath
    val cAdapterHeaderName      get() = cAdapterHeader.absolutePath
    val cAdapterCppName         get() = cAdapterCpp.absolutePath
    val cAdapterBitcodeName     get() = cAdapterBitcode.absolutePath
}

