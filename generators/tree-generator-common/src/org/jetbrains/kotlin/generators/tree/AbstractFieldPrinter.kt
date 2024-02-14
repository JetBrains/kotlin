/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.lang.reflect.Modifier.isVolatile

abstract class AbstractFieldPrinter<Field : AbstractField<*>>(
    private val printer: SmartPrinter,
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

    context(ImportCollector)
    fun printField(
        field: Field,
        override: Boolean,
        inConstructor: Boolean = false,
        modality: Modality? = null,
    ) {
        printer.run {
            val defaultValue = field.defaultValueInImplementation
            printPropertyDeclaration(
                name = field.name,
                type = actualTypeOfField(field),
                kind = if (forceMutable(field) || field.isFinal && field.isMutable) VariableKind.VAR else VariableKind.VAL,
                inConstructor = inConstructor,
                visibility = field.visibility,
                modality = modality,
                override = override,
                isLateinit = field.isLateinit,
                isVolatile = field.isVolatile,
                optInAnnotation = field.optInAnnotation,
                printOptInWrapped = defaultValue != null,
                deprecation = field.deprecation,
                kDoc = field.kDoc,
                initializer = defaultValue.takeUnless { field.withGetter }
            )
            println()

            if (defaultValue != null && field.withGetter) {
                withIndent {
                    println("get() = $defaultValue")
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
