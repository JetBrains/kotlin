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
import org.jetbrains.kotlin.backend.common.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import java.util.*

internal val fileClassPhase = makeIrFilePhase(
    ::FileClassLowering,
    name = "FileClass",
    description = "Put file level function and property declaration into a class",
    stickyPostconditions = setOf(::checkAllFileLevelDeclarationsAreClasses)
)

private fun checkAllFileLevelDeclarationsAreClasses(irFile: IrFile) {
    assert(irFile.declarations.all { it is IrClass })
}

private class FileClassLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val classes = ArrayList<IrClass>()
        val fileClassMembers = ArrayList<IrDeclaration>()

        irFile.declarations.forEach {
            if (it is IrClass)
                classes.add(it)
            else
                fileClassMembers.add(it)
        }

        if (fileClassMembers.isEmpty() && irFile.metadata?.descriptors.isNullOrEmpty()) return

        val irFileClass = createFileClass(irFile, fileClassMembers)
        classes.add(irFileClass)

        irFile.declarations.clear()
        irFile.declarations.addAll(classes)
    }

    private fun createFileClass(irFile: IrFile, fileClassMembers: List<IrDeclaration>): IrClass {
        val fileEntry = irFile.fileEntry
        val ktFile = context.psiSourceManager.getKtFile(fileEntry as PsiSourceManager.PsiFileEntry)
            ?: throw AssertionError("Unexpected file entry: $fileEntry")
        val fileClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(ktFile)
        val descriptor = WrappedClassDescriptor(sourceElement = KotlinSourceElement(ktFile))
        return IrClassImpl(
            0, fileEntry.maxOffset,
            IrDeclarationOrigin.FILE_CLASS,
            symbol = IrClassSymbolImpl(descriptor),
            name = fileClassInfo.fileClassFqName.shortName(),
            kind = ClassKind.CLASS,
            visibility = Visibilities.PUBLIC,
            modality = Modality.FINAL,
            isCompanion = false,
            isInner = false,
            isData = false,
            isExternal = false,
            isInline = false
        ).apply {
            descriptor.bind(this)
            superTypes.add(context.irBuiltIns.anyType)
            parent = irFile
            declarations.addAll(fileClassMembers)
            createImplicitParameterDeclarationWithWrappedDescriptor()
            // TODO: figure out why reparenting leads to failing tests.
            // fileClassMembers.forEach { it.parent = this }
            metadata = irFile.metadata

            val partClassType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(fileClassInfo.fileClassFqName)
            val facadeClassType =
                if (fileClassInfo.withJvmMultifileClass) AsmUtil.asmTypeByFqNameWithoutInnerClasses(fileClassInfo.facadeClassFqName)
                else null
            context.state.factory.packagePartRegistry.addPart(irFile.fqName, partClassType.internalName, facadeClassType?.internalName)
        }
        // TODO file annotations
    }
}
