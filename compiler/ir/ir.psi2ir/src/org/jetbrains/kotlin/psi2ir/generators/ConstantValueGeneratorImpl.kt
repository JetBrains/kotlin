/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.util.ConstantValueGenerator
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.lazy.descriptors.getSourceForArgument
import org.jetbrains.kotlin.resolve.source.getPsi

class ConstantValueGeneratorImpl(
    moduleDescriptor: ModuleDescriptor,
    symbolTable: ReferenceSymbolTable,
    typeTranslator: TypeTranslator,
    allowErrorTypeInAnnotations: Boolean,
) : ConstantValueGenerator(moduleDescriptor, symbolTable, typeTranslator, allowErrorTypeInAnnotations) {
    override fun extractAnnotationOffsets(annotationDescriptor: AnnotationDescriptor): Pair<Int, Int> =
        extractOffsets(annotationDescriptor.source)

    override fun extractAnnotationParameterOffsets(annotationDescriptor: AnnotationDescriptor, argumentName: Name): Pair<Int, Int> =
        extractOffsets(annotationDescriptor.getSourceForArgument(argumentName))

    private fun extractOffsets(sourceElement: SourceElement): Pair<Int, Int> {
        val psi = sourceElement.getPsi()
        if (psi == null || psi.containingFile.fileType.isBinary) return UNDEFINED_OFFSET to UNDEFINED_OFFSET
        return Pair(psi.startOffset, psi.endOffset)
    }
}
