/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.generators.util.printBlock
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
            val defaultValue = if (inImplementation)
                field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
            else null
            val shouldBeParameter = inConstructor && field.customSetter != null
            printPropertyDeclaration(
                name = field.name,
                type = actualTypeOfField(field),
                kind = when {
                    shouldBeParameter -> VariableKind.PARAMETER
                    forceMutable(field) || field.isFinal && field.isMutable -> VariableKind.VAR
                    else -> VariableKind.VAL
                },
                inConstructor = inConstructor,
                visibility = field.visibility,
                modality = modality.takeUnless { shouldBeParameter },
                override = override && !shouldBeParameter,
                isLateinit = !shouldBeParameter && (inImplementation || field.isFinal) && field.implementationDefaultStrategy is AbstractField.ImplementationDefaultStrategy.Lateinit,
                isVolatile = !shouldBeParameter && (inImplementation || field.isFinal) && field.isVolatile,
                optInAnnotation = field.optInAnnotation,
                printOptInWrapped = wrapOptInAnnotations && defaultValue != null,
                deprecation = field.deprecation,
                kDoc = field.kDoc.takeIf { !inImplementation },
                initializer = when {
                    defaultValue?.withGetter == true -> null
                    defaultValue != null -> defaultValue.defaultValue
                    !inConstructor && field.customSetter != null -> field.name
                    else -> null
                }
            )
            println()

            if (defaultValue != null && defaultValue.withGetter) {
                withIndent {
                    println("get() = ${defaultValue.defaultValue}")
                }
            }

            if (inImplementation && !inConstructor) {
                field.customSetter?.let {
                    withIndent {
                        print("set(value)")
                        printBlock {
                            printlnMultiLine(it)
                        }
                    }
                }
            }
        }
    }
}
