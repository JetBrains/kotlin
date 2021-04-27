/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.preprocessing

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.synthetics.findClassDescriptor
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

class SourceDeclarationsPreprocessor(private val context: GeneratorContext) {

    fun run(ktFiles: Collection<KtFile>, ktToIr: Map<KtFile, IrFile>) {
        for (ktFile in ktFiles.toSet()) {
            val irFile = ktToIr[ktFile] ?: error("No ir file found for ${ktFile.name}")
            context.symbolTable.signaturer.inFile(irFile.symbol) {
                for (ktDeclaration in ktFile.declarations) {
                    processDeclaration(ktDeclaration)
                }
            }
        }
    }

    private fun processDeclaration(ktDeclaration: KtDeclaration) {
        when (ktDeclaration) {
            is KtClassOrObject ->
                processClassOrObject(ktDeclaration)
        }
    }

    private fun processClassOrObject(ktClassOrObject: KtClassOrObject) {
        val classDescriptor = ktClassOrObject.findClassDescriptor(context.bindingContext)
        if (DescriptorUtils.isEnumEntry(classDescriptor)) return
        context.symbolTable.referenceClass(classDescriptor)
        ktClassOrObject.body?.let { ktClassBody ->
            ktClassBody.declarations.forEach { processDeclaration(it) }
        }
    }
}
