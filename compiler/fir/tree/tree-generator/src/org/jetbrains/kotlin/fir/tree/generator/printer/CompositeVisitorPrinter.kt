/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.PrimaryConstructorParameter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.types.Variance

internal class CompositeVisitorPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val implementationKind: ImplementationKind
        get() = ImplementationKind.FinalClass

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>> =
        listOfNotNull(firVisitorType.withArgs(StandardTypes.unit, dataTypeVariable))

    override val constructorParameters: List<PrimaryConstructorParameter> =
        listOf(PrimaryConstructorParameter(FunctionParameter("visitors", StandardTypes.array.withArgs(TypeRefWithVariance(Variance.OUT_VARIANCE, firVisitorType.withArgs(StandardTypes.unit, dataTypeVariable)))), VariableKind.VAL))

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    override fun printMethodsForElement(element: Element) {
        printer.run {
            printMethodDeclarationForElement(element, modality = null, override = true)
            printBlock {
                println("visitors.forEach { it.${element.visitFunctionName}(${element.visitorParameterName}, data) }")
            }
        }
    }

}
