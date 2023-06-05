/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.util.transformFlat

fun moveOpenClassesToSeparateFiles(moduleFragment: IrModuleFragment) {
    fun createFile(file: IrFile, klass: IrClass): IrFile =
        IrFileImpl(fileEntry = file.fileEntry, fqName = file.packageFqName, symbol = IrFileSymbolImpl(), module = file.module).also {
            it.annotations = it.annotations memoryOptimizedPlus file.annotations
            it.declarations += klass
            klass.parent = it
        }

    moduleFragment.files.transformFlat { file ->
        // We don't have to split declarations with a single class
        if (file.declarations.size <= 1)
            return@transformFlat null

        val openClasses = mutableListOf<IrClass>()
        fun removeAndCollectOpenClasses(container: IrDeclarationContainer) {
            container.transformDeclarationsFlat { declaration ->
                if (declaration is IrDeclarationContainer) {
                    removeAndCollectOpenClasses(declaration)
                }
                if (
                    declaration is IrClass &&
                    (declaration.modality == Modality.OPEN || declaration.modality == Modality.ABSTRACT) &&
                    declaration.kind == ClassKind.CLASS &&
                    declaration.visibility != DescriptorVisibilities.PRIVATE &&
                    declaration.visibility != DescriptorVisibilities.LOCAL
                ) {
                    openClasses += declaration
                    emptyList()
                } else {
                    null
                }
            }
        }
        removeAndCollectOpenClasses(file)

        return@transformFlat if (openClasses.isEmpty())
            null
        else
            openClasses.mapTo(mutableListOf(file)) { createFile(file, it) }
    }
}

