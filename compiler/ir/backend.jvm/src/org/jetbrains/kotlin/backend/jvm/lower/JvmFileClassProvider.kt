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

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import java.lang.AssertionError
import java.util.*

class JvmFileClassProvider : Psi2IrTranslator.PostprocessingStep {
    override fun postprocess(context: GeneratorContext, irElement: IrElement) {
        when (irElement) {
            is IrModuleFragment ->
                irElement.files.forEach { postprocess(context, it) }
            is IrFile ->
                recordFileClassInfo(context, irElement)
        }
    }

    private val fileClassInfoByFileEntry = HashMap<SourceManager.FileEntry, JvmFileClassInfo>()
    private val sourceElementByFileEntry = HashMap<SourceManager.FileEntry, SourceElement>()

    private fun recordFileClassInfo(context: GeneratorContext, irFile: IrFile) {
        context.sourceManager.getKtFile(irFile)?.let { ktFile ->
            sourceElementByFileEntry[irFile.fileEntry] = KotlinSourceElement(ktFile)
        }
        val jvmFileClassInfo = context.sourceManager.getFileClassInfo(irFile) ?: return
        fileClassInfoByFileEntry[irFile.fileEntry] = jvmFileClassInfo
    }

    private fun PsiSourceManager.getFileClassInfo(irFile: IrFile): JvmFileClassInfo? {
        val file = getKtFile(irFile) ?: return null
        return JvmFileClassUtil.getFileClassInfoNoResolve(file)
    }

    fun createFileClassDescriptor(fileEntry: SourceManager.FileEntry, packageFragment: PackageFragmentDescriptor): FileClassDescriptor {
        val fileClassInfo = fileClassInfoByFileEntry[fileEntry] ?: throw AssertionError("No file class info for ${fileEntry.name})")
        val sourceElement = sourceElementByFileEntry[fileEntry] ?: SourceElement.NO_SOURCE
        return FileClassDescriptorImpl(fileClassInfo.fileClassFqName.shortName(), packageFragment, sourceElement)
    }
}
