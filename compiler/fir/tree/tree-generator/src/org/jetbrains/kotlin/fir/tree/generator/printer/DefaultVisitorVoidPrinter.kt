/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.firVisitorVoidType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.SmartPrinter

internal class DefaultVisitorVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : VisitorPrinter(printer, visitorType, true) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = firVisitorVoidType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    context(ImportCollector)
    override fun SmartPrinter.printVisitMethod(element: Element, modality: Modality?, override: Boolean) {
        printVisitMethodDeclaration(
            element,
            hasDataParameter = false,
            modality = modality,
            override = override,
        )
        val parentInVisitor = parentInVisitor(element)
        if (parentInVisitor != null) {
            println(" = ", parentInVisitor.visitFunctionName, "(", element.visitorParameterName, element.castToFirElementIfNeeded(), ")")
        }
        println()
    }
}
