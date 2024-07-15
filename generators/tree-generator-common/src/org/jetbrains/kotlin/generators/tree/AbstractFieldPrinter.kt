/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractFieldPrinter<Field : AbstractField<*>>(
    private val printer: ImportCollectingPrinter,
) {

    /**
     * Allows to forcibly make the field a `var` instead of `val`.
     */
    protected open fun forceMutable(field: Field): Boolean = false

    /**
     * Allows to override the printed type of [field]. For example, for list fields we may want to use [MutableList] instead of [List]
     * in implementation classes.
     */
    protected open fun actualTypeOfField(field: Field): TypeRefWithNullability = field.typeRef

    protected open val wrapOptInAnnotations: Boolean
        get() = false

    fun printField(
        field: Field,
        inImplementation: Boolean,
        override: Boolean,
        inConstructor: Boolean = false,
        modality: Modality? = null,
    ) {
        printer.run {
            val fieldImplementation = field.implementation
            printPropertyDeclaration(
                name = field.name,
                type = actualTypeOfField(field),
                kind = if ((fieldImplementation as? AbstractField.ImplementationStrategy.Property)?.isMutable == true)
                    VariableKind.VAR else VariableKind.VAL,
                inConstructor = inConstructor,
                visibility = field.visibility,
                modality = modality,
                override = override,
                isLateinit = fieldImplementation is AbstractField.ImplementationStrategy.LateinitField,
                isVolatile = fieldImplementation is AbstractField.ImplementationStrategy.Property && field.isVolatile,
                optInAnnotation = field.optInAnnotation,
                printOptInWrapped = wrapOptInAnnotations &&
                        fieldImplementation is AbstractField.ImplementationStrategy.RegularField && fieldImplementation.defaultValue != null,
                deprecation = field.deprecation,
                kDoc = field.kDoc.takeIf { !override && !inImplementation },
                initializer = (fieldImplementation as? AbstractField.ImplementationStrategy.RegularField)?.defaultValue
            )
            println()

            if (fieldImplementation is AbstractField.ImplementationStrategy.ComputedProperty) {
                withIndent {
                    println("get() = ${fieldImplementation.defaultValue}")
                }
            }

            field.customSetter?.let {
                withIndent {
                    print("set(value)")
                    printBlock {
                        println(it)
                    }
                }
            }
        }
    }
}
