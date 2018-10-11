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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import java.util.*

class FileClassLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val classes = ArrayList<IrClass>()
        val fileClassMembers = ArrayList<IrDeclaration>()

        irFile.declarations.forEach {
            if (it is IrClass)
                classes.add(it)
            else
                fileClassMembers.add(it)
        }

        if (fileClassMembers.isEmpty()) return

        val fileClassDescriptor = context.declarationFactory.createFileClassDescriptor(irFile.fileEntry, irFile.packageFragmentDescriptor)
        val irFileClass = IrClassImpl(0, irFile.fileEntry.maxOffset, IrDeclarationOrigin.DEFINED, fileClassDescriptor, fileClassMembers)
        classes.add(irFileClass)

        irFile.declarations.clear()
        irFile.declarations.addAll(classes)
    }
}

