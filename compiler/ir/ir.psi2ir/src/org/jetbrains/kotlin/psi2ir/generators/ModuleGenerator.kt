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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

class ModuleGenerator(override val context: GeneratorContext) : Generator {
    fun generateModule(ktFiles: List<KtFile>): IrModule {
        val irDeclarationGenerator = DeclarationGenerator(context)

        val irModule = IrModuleImpl(context.moduleDescriptor, context.irBuiltIns)

        for (ktFile in ktFiles) {
            val fileEntry = context.sourceManager.getOrCreateFileEntry(ktFile)
            val packageFragmentDescriptor = getOrFail(BindingContext.FILE_TO_PACKAGE_FRAGMENT, ktFile)
            val fileName = fileEntry.getRecognizableName()
            val irFile = IrFileImpl(fileEntry, fileName, packageFragmentDescriptor)
            context.sourceManager.putFileEntry(irFile, fileEntry)

            for (ktAnnotationEntry in ktFile.annotationEntries) {
                irFile.addAnnotation(getOrFail(BindingContext.ANNOTATION, ktAnnotationEntry))
            }

            for (ktDeclaration in ktFile.declarations) {
                irFile.addDeclaration(irDeclarationGenerator.generateMemberDeclaration(ktDeclaration))
            }

            irModule.addFile(irFile)
        }
        return irModule
    }

}