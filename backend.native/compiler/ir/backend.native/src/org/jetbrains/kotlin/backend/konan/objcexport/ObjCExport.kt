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

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal class ObjCExport(val context: Context) {
    internal fun produceObjCFramework() {
        if (context.config.produce != CompilerOutputKind.FRAMEWORK) return

        val headerGenerator = ObjCExportHeaderGenerator(context)
        headerGenerator.translateModule()

        val namer = headerGenerator.namer
        val mapper = headerGenerator.mapper

        val framework = File(context.config.outputFile)
        val headers = framework.child("Headers")

        val frameworkName = framework.name.removeSuffix(".framework")
        val headerName = frameworkName + ".h"
        val header = headers.child(headerName)
        headers.mkdirs()
        header.writeLines(headerGenerator.build())

        val modules = framework.child("Modules")
        modules.mkdirs()

        val moduleMap = """
            |framework module $frameworkName {
            |    umbrella header "$headerName"
            |
            |    export *
            |    module * { export * }
            |}
        """.trimMargin()

        modules.child("module.modulemap").writeBytes(moduleMap.toByteArray())

        val objCCodeGenerator = ObjCExportCodeGenerator(CodeGenerator(context), namer, mapper)
        objCCodeGenerator.emitRtti(headerGenerator.generatedClasses, headerGenerator.topLevel)
    }
}
