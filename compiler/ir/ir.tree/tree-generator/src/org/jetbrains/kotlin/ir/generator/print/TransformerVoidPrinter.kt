/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.irLeafTransformerVoidType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.utils.withIndent

internal class TransformerVoidPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractTransformerVoidPrinter<Element, Field>(printer) {

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irLeafTransformerVoidType)

    // IrPackageFragment is treated as transformByChildren in IrElementTransformerVoid for historical reasons.
    private val Element.isPackageFragment: Boolean
        get() = this == IrTree.packageFragment

    // Despite IrFile and IrExternalPackageFragment being transformByChildren, we treat them differently in IrElementTransformerVoid
    // than in IrTransformer for historical reasons. We want to preserve the historical semantics here.
    private val Element.isPackageFragmentChild: Boolean
        get() = elementParents.any { it.element.isPackageFragment }

    private val Element.transformByChildrenVoid: Boolean
        get() = element.transformByChildren || isPackageFragment

    override fun visitMethodReturnType(element: Element): Element =
        when {
            element.isPackageFragment -> element
            element.transformByChildren -> element.getTransformExplicitType()
            else -> element.parentInVisitor?.let(this::visitMethodReturnType) ?: element
        }

    override fun overrideVisitMethod(element: Element): Boolean =
        element.hasAcceptMethod

    override fun visitMethodModality(element: Element): Modality? =
        if (overrideVisitMethod(element)) {
            null
        } else {
            Modality.OPEN
        }

    override fun printMethodsForElement(element: Element) {
        if (element.isRootElement) return
        val parent = element.parentInVisitor
        if (!element.transformByChildrenVoid && parent == null) return
        printer.run {
            println()
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                modality = visitMethodModality(element),
                override = overrideVisitMethod(element),
            )
            println(" =")
            withIndent {
                if (element.transformByChildrenVoid && !element.isPackageFragmentChild) {
                    println("transformElement(", element.visitorParameterName, ")")
                } else {
                    print(parent!!.visitFunctionName, "(", element.visitorParameterName, ")")
                    if (element.isPackageFragmentChild) {
                        print(" as ", element.render())
                    }
                    println()
                }
            }
        }
    }
}
