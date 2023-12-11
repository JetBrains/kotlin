/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.utils.SmartPrinter

abstract class AbstractVisitorVoidPrinter<Element, Field>(
    printer: SmartPrinter,
) : AbstractVisitorPrinter<Element, Field>(printer)
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {

    final override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    final override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    abstract val visitorSuperClass: ClassRef<PositionTypeParameterRef>

    final override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = visitorSuperClass.withArgs(StandardTypes.unit, visitorDataType)

    abstract val useAbstractMethodForRootElement: Boolean

    abstract val overriddenVisitMethodsAreFinal: Boolean

    context(ImportCollector)
    final override fun printMethodsForElement(element: Element) {
        val parentInVisitor = parentInVisitor(element)
        if (!element.isRootElement && parentInVisitor == null) return

        val isAbstractVisitRootElementMethod = element.isRootElement && useAbstractMethodForRootElement

        printMethodDeclarationForElement(
            element,
            modality = Modality.FINAL.takeIf { overriddenVisitMethodsAreFinal },
            override = true,
        )

        fun SmartPrinter.printBody(parentInVisitor: Element?) {
            printBlock {
                if (parentInVisitor != null) {
                    println(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ")")
                }
            }
        }

        printer.run {
            printBody(element)
            println()
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                modality = when {
                    isAbstractVisitRootElementMethod && visitorType.kind == TypeKind.Class -> Modality.ABSTRACT
                    !isAbstractVisitRootElementMethod && visitorType.kind == TypeKind.Class -> Modality.OPEN
                    else -> null
                }
            )
            if (isAbstractVisitRootElementMethod) {
                println()
            } else {
                printBody(parentInVisitor)
            }
        }
    }
}