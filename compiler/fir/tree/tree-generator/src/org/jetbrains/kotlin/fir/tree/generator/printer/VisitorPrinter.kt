/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.SmartPrinter

internal class VisitorPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    private val visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>? =
        firVisitorType.takeIf { visitSuperTypeByDefault }?.withArgs(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = resultTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun skipElement(element: Element): Boolean = visitSuperTypeByDefault && element.isRootElement

    override fun parentInVisitor(element: Element): Element? = when {
        element.isRootElement -> null
        visitSuperTypeByDefault -> element.parentInVisitor
        else -> AbstractFirTreeBuilder.baseFirElement
    }
}
