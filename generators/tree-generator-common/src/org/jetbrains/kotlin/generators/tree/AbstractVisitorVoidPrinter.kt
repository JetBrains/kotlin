/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractVisitorVoidPrinter<Element, Field>(
    printer: SmartPrinter,
    visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault)
        where Element : AbstractElement<Element, Field>,
              Field : AbstractField {

    final override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    abstract val visitorSuperClass: ClassRef<PositionTypeParameterRef>

    final override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = if (visitSuperTypeByDefault)
            visitorSuperClass
        else
            visitorSuperClass.withArgs(StandardTypes.unit, StandardTypes.nothing.copy(nullable = true))

    abstract val useAbstractMethodForRootElement: Boolean

    abstract val overriddenVisitMethodsAreFinal: Boolean

    final override fun printMethodsForElement(element: Element) {
        val parentInVisitor = parentInVisitor(element)
        if (!element.isRootElement && parentInVisitor == null) return

        val isAbstractVisitRootElementMethod = element.isRootElement && useAbstractMethodForRootElement

        if (!isAbstractVisitRootElementMethod) {
            printMethodDeclarationForElement(
                element,
                modality = if (overriddenVisitMethodsAreFinal) Modality.FINAL else Modality.OPEN,
                override = true,
            )
        }

        fun SmartPrinter.printBody(parentInVisitor: Element?) {
            if (!isAbstractVisitRootElementMethod) {
                println(" {")
                if (parentInVisitor != null) {
                    withIndent {
                        println(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ")")
                    }
                }
                println("}")
            }
        }

        printer.run {
            printBody(element)
            println()
            printVisitMethodDeclaration(
                element,
                additionalParameters = emptyList(),
                returnType = StandardTypes.unit,
                modality = when {
                    element.isRootElement && visitorType.kind == TypeKind.Class -> Modality.ABSTRACT
                    !element.isRootElement && visitorType.kind == TypeKind.Class -> Modality.OPEN
                    else -> Modality.FINAL
                }
            )
            if (isAbstractVisitRootElementMethod) {
                println()
            }
            printBody(parentInVisitor)
        }
    }
}