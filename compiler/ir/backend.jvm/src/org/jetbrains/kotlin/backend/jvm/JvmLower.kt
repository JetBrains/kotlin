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

import org.jetbrains.kotlin.backend.jvm.lower.FileClassLowering
import org.jetbrains.kotlin.backend.jvm.lower.InitializersLowering
import org.jetbrains.kotlin.backend.jvm.lower.PropertiesLowering
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile

class JvmLower(val context: JvmBackendContext) {
    fun lower(irFile: IrFile) {
        FileClassLowering(context.jvmFileClassProvider).lower(irFile)
        PropertiesLowering().lower(irFile)
        InitializersLowering().runOnNormalizedFile(irFile)
    }
}

interface FileLoweringPass {
    fun lower(irFile: IrFile
    )
}

interface ClassLoweringPass {
    fun lower(irClass: IrClass)
}

fun ClassLoweringPass.runOnNormalizedFile(irFile: IrFile) {
    fun runPostfix(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.forEach {
            if (it is IrClass) {
                runPostfix(it)
                lower(it)
            }
        }
    }

    runPostfix(irFile)
}