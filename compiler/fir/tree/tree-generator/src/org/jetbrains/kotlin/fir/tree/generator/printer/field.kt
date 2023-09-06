/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.AbstractFieldPrinter
import org.jetbrains.kotlin.utils.SmartPrinter

internal class FieldPrinter(printer: SmartPrinter) : AbstractFieldPrinter<Field>(printer) {
    override fun isVal(field: Field): Boolean = field.isVal

    override fun getType(field: Field, isImplementation: Boolean, notNull: Boolean): String =
        if (isImplementation) field.getMutableType(notNull = notNull) else field.getTypeWithArguments(notNull = notNull)
}

fun SmartPrinter.printField(
    field: Field,
    isImplementation: Boolean,
    override: Boolean,
    inConstructor: Boolean = false,
    notNull: Boolean = false,
    modifiers: SmartPrinter.() -> Unit = {},
) {
    FieldPrinter(this).printField(field, isImplementation, override, inConstructor, notNull, defaultValue = null, modifiers)
}

fun SmartPrinter.printFieldWithDefaultInImplementation(field: Field) {
    val defaultValue = field.defaultValueInImplementation
    requireNotNull(defaultValue) {
        "No default value for $field"
    }
    FieldPrinter(this).printField(field, isImplementation = true, override = true, notNull = false, defaultValue = defaultValue)
}
