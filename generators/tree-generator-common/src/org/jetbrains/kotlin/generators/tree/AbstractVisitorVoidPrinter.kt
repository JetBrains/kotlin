/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock

abstract class AbstractVisitorVoidPrinter<Element, Field>(
    printer: ImportCollectingPrinter,
) : AbstractVisitorPrinter<Element, Field>(printer)
        where Element : AbstractElement<Element, Field, *>,
              Field : AbstractField<Field> {

    final override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    final override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    abstract val visitorSuperClass: ClassRef<PositionTypeParameterRef>

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(visitorSuperClass.withArgs(StandardTypes.unit, visitorDataType))

    abstract val useAbstractMethodForRootElement: Boolean

    abstract val overriddenVisitMethodsAreFinal: Boolean

    protected open fun shouldOverrideMethodWithNoDataParameter(element: Element): Boolean = false

    final override fun printMethodsForElement(element: Element) {
        val parentInVisitor = parentInVisitor(element)
        if (!element.isRootElement && parentInVisitor == null) return

        val isAbstractVisitRootElementMethod = element.isRootElement && useAbstractMethodForRootElement

        printMethodDeclarationForElement(
            element,
            modality = Modality.FINAL.takeIf { overriddenVisitMethodsAreFinal },
            override = true,
        )

        fun ImportCollectingPrinter.printBody(parentInVisitor: Element?) {
            printBlock {
                if (parentInVisitor != null) {
                    println(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ")")
                }
            }
        }

        printer.run {
            printBody(element)
            println()
            val override = shouldOverrideMethodWithNoDataParameter(element)
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                modality = when {
                    isAbstractVisitRootElementMethod && visitorType.kind == TypeKind.Class -> Modality.ABSTRACT
                    !override && !isAbstractVisitRootElementMethod && visitorType.kind == TypeKind.Class -> Modality.OPEN
                    else -> null
                },
                override = override,
            )
            if (isAbstractVisitRootElementMethod) {
                println()
            } else {
                printBody(parentInVisitor)
            }
        }
    }
}