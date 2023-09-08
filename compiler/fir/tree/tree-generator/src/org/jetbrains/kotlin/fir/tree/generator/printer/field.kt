/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.utils.SmartPrinter


fun SmartPrinter.printField(field: Field, isImplementation: Boolean, override: Boolean, inConstructor: Boolean = false, notNull: Boolean = false, modifiers: SmartPrinter.() -> Unit = {}) {
    if (!field.isVal && field.isVolatile) {
        println("@Volatile")
    }

    field.optInAnnotation?.let {
        println(if (inConstructor) "@property:${it.type}" else "@${it.type}")
    }

    modifiers()

    if (override) {
        print("override ")
    }
    if (field.isLateinit) {
        print("lateinit ")
    }
    if (isImplementation && !field.isVal || field.isFinal && field.isMutable) {
        print("var")
    } else {
        print("val")
    }
    val type = if (isImplementation) field.getMutableType(notNull = notNull) else field.getTypeWithArguments(notNull = notNull)
    print(" ${field.name}: $type")
    if (inConstructor) print(",")
    println()
}

fun SmartPrinter.printFieldWithDefaultInImplementation(field: Field) {
    if (!field.isVal && field.isVolatile) {
        println("@Volatile")
    }
    field.optInAnnotation?.let {
        println("@OptIn(${it.type}::class)")
    }
    val defaultValue = field.defaultValueInImplementation
    print("override ")
    if (field.isVal) {
        print("val")
    } else {
        print("var")
    }
    print(" ${field.name}: ${field.getMutableType()} ")
    if (field.withGetter) {
        if (field.customSetter != null) {
            println()
            pushIndent()
        }
        print("get() ")
    }
    requireNotNull(defaultValue) {
        "No default value for $field"
    }
    println("= $defaultValue")
    field.customSetter?.let {
        println("set(value) {")
        pushIndent()
        println(it)
        popIndent()
        println("}")
        popIndent()
    }
}
