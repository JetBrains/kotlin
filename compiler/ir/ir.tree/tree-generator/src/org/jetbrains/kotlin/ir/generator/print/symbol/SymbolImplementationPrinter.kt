/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print.symbol

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolField
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolImplementation

class SymbolImplementationPrinter(
    printer: ImportCollectingPrinter,
) : AbstractImplementationPrinter<SymbolImplementation, Symbol, SymbolField>(printer) {

    override val implementationOptInAnnotation: ClassRef<*>
        get() = obsoleteDescriptorBasedApiAnnotation

    override fun getPureAbstractElementType(implementation: SymbolImplementation): ClassRef<*> =
        (if (implementation.hasSignature) irSymbolWithSignatureType else irSymbolBaseType)
            .withArgs(implementation.element.descriptor!!, implementation.element.owner!!)

    override fun makeFieldPrinter(printer: ImportCollectingPrinter) = object : AbstractFieldPrinter<SymbolField>(printer) {}

    override fun ImportCollecting.parentConstructorArguments(implementation: SymbolImplementation): List<String> = buildList {
        add("descriptor")
        if (implementation.hasSignature) {
            add("signature")
        }
    }

    override fun additionalConstructorParameters(implementation: SymbolImplementation): List<FunctionParameter> = buildList {
        add(FunctionParameter("descriptor", implementation.element.descriptor!!.copy(nullable = true), "null"))
        if (implementation.hasSignature) {
            add(FunctionParameter("signature", idSignatureType.copy(nullable = true), "null"))
        }
    }

    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: SymbolImplementation) {
        implementation.generationCallback?.invoke(this)
    }
}