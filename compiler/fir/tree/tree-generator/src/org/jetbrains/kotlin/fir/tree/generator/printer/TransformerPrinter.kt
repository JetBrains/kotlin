/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.FirTree
import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter

internal class TransformerPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(firVisitorType.withArgs(rootElement, visitorDataType))

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun parentInVisitor(element: Element) = when {
        element.isRootElement -> null
        else -> rootElement
    }
}
