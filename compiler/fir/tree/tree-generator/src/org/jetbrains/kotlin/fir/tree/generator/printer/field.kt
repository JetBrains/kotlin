/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.generators.tree.render
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

context(ImportCollector)
fun SmartPrinter.printField(
    field: Field,
    isImplementation: Boolean,
    override: Boolean,
    inConstructor: Boolean = false,
    modifiers: SmartPrinter.() -> Unit = {},
) {
    if (!override && field.kDoc != null) {
        println()
        printKDoc(field.kDoc)
    }
    if (!field.isVal && field.isVolatile) {
        println("@Volatile")
    }

    field.optInAnnotation?.let {
        val rendered = it.render()
        println(if (inConstructor) "@property:$rendered" else "@$rendered")
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
    val type = if (isImplementation) field.getMutableType() else field.typeRef
    print(" ${field.name}: ${type.render()}")
    if (inConstructor) print(",")
    println()
}

context(ImportCollector)
fun SmartPrinter.printFieldWithDefaultInImplementation(field: Field) {
    if (!field.isVal && field.isVolatile) {
        println("@Volatile")
    }
    field.optInAnnotation?.let {
        println("@OptIn(${it.render()}::class)")
    }
    val defaultValue = field.defaultValueInImplementation
    print("override ")
    if (field.isVal) {
        print("val")
    } else {
        print("var")
    }
    print(" ${field.name}: ${field.getMutableType().render()}")
    if (field.withGetter) {
        println()
        pushIndent()
        print("get()")
    }
    requireNotNull(defaultValue) {
        "No default value for $field"
    }
    println(" = $defaultValue")
    field.customSetter?.let {
        println("set(value) {")
        withIndent {
            println(it)
        }
        println("}")
    }
    if (field.withGetter) {
        popIndent()
    }
}
