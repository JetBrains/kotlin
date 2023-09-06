/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.utils.SmartPrinter

abstract class AbstractFieldPrinter<Field : AbstractField>(
    private val printer: SmartPrinter,
) {
    protected abstract fun isVal(field: Field): Boolean

    protected abstract fun getType(field: Field, isImplementation: Boolean, notNull: Boolean): String

    fun printField(
        field: Field,
        isImplementation: Boolean,
        override: Boolean,
        inConstructor: Boolean = false,
        notNull: Boolean = false,
        defaultValue: String? = null,
        modifiers: SmartPrinter.() -> Unit = {},
    ) {
        printer.run {
            if (!isVal(field) && field.isVolatile) {
                println("@Volatile")
            }

            field.optInAnnotation?.let {
                when {
                    isImplementation && defaultValue != null -> println("@OptIn(${it.type}::class)")
                    inConstructor -> println("@property:${it.type}")
                    else -> println("@${it.type}")
                }
            }

            modifiers()

            if (override) {
                print("override ")
            }
            if (field.isLateinit) {
                print("lateinit ")
            }
            if (isImplementation && !isVal(field) || field.isFinal && field.isMutable) {
                print("var ")
            } else {
                print("val ")
            }
            val type = getType(field, isImplementation, notNull)
            print(field.name, ": ", type)
            if (inConstructor) {
                print(",")
            }
            if (defaultValue == null) {
                println()
                return
            }
            print(" ")

            if (field.withGetter) {
                if (field.customSetter != null) {
                    println()
                    pushIndent()
                }
                print("get() ")
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
    }
}