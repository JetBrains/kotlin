/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.render
import java.lang.AssertionError

class JvmBackend(val context: JvmBackendContext) {
    private val lower = JvmLower(context)
    private val codegen = JvmCodegen(context)

    fun generateFile(irFile: IrFile) {
        val extensions = IrGenerationExtension.getInstances(context.state.project)
        extensions.forEach { it.generate(irFile, context, context.state.bindingContext) }

        lower.lower(irFile)

        for (loweredClass in irFile.declarations) {
            if (loweredClass !is IrClass) {
                throw AssertionError("File-level declaration should be IrClass after JvmLower, got: " + loweredClass.render())
            }

            codegen.generateClass(loweredClass)
        }
    }
}