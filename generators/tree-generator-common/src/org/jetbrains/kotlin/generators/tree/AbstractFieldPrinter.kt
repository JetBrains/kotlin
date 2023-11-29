/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

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

    protected open val wrapOptInAnnotations: Boolean
        get() = false

    context(ImportCollector)
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
            printPropertyDeclaration(
                name = field.name,
                type = actualTypeOfField(field),
                kind = if (forceMutable(field) || field.isFinal && field.isMutable) VariableKind.VAR else VariableKind.VAL,
                inConstructor = inConstructor,
                visibility = field.visibility,
                modality = modality,
                override = override,
                isLateinit = (inImplementation || field.isFinal) && field.implementationDefaultStrategy is AbstractField.ImplementationDefaultStrategy.Lateinit,
                isVolatile = (inImplementation || field.isFinal) && field.isVolatile,
                optInAnnotation = field.optInAnnotation,
                printOptInWrapped = wrapOptInAnnotations && defaultValue != null,
                deprecation = field.deprecation,
                kDoc = field.kDoc.takeIf { !inImplementation },
                initializer = defaultValue?.takeUnless { it.withGetter }?.defaultValue
            )
            println()

            if (defaultValue != null && defaultValue.withGetter) {
                withIndent {
                    println("get() = ${defaultValue.defaultValue}")
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

context(ImportCollector)
fun SmartPrinter.printField(
    field: AbstractField<*>,
    type: TypeRef = field.typeRef,
    isMutable: Boolean? = field.isMutable,
    override: Boolean = false,
    modality: Modality? = null,
    inConstructor: Boolean = false,
    kDoc: String? = field.kDoc,
    optInAnnotation: ClassRef<*>? = field.optInAnnotation,
) = printPropertyHeader(
    field.name, type, isMutable,
    override = override,
    inConstructor = inConstructor,
    visibility = field.visibility,
    modality = modality,
    isLateinit = field.isLateinit,
    isVolatile = field.isVolatile,
    kDoc = kDoc,
    optInAnnotation = optInAnnotation,
    deprecation = field.deprecation,
)

context(ImportCollector)
fun SmartPrinter.printPropertyHeader(
    name: String,
    type: TypeRef,
    isMutable: Boolean?,
    override: Boolean = false,
    inConstructor: Boolean = false,
    visibility: Visibility = Visibility.PUBLIC,
    modality: Modality? = null,
    isLateinit: Boolean = false,
    isVolatile: Boolean = false,
    kDoc: String? = null,
    optInAnnotation: ClassRef<*>? = null,
    deprecation: Deprecated? = null,
) {
    printKDoc(kDoc)

    deprecation?.let {
        println("@Deprecated(")
        withIndent {
            println("message = \"", it.message, "\",")
            println("replaceWith = ReplaceWith(\"", it.replaceWith.expression, "\"),")
            println("level = DeprecationLevel.", it.level.name, ",")
        }
        println(")")
    }

    if (isVolatile) {
        println("@", type<Volatile>().render())
    }

    optInAnnotation?.let {
        val rendered = it.render()
        when {
            inConstructor -> println("@property:", rendered)
            else -> println("@", rendered)
        }
    }

    if (visibility != Visibility.PUBLIC) {
        print(visibility.name.toLowerCaseAsciiOnly(), " ")
    }

    modality?.let {
        print(it.name.toLowerCaseAsciiOnly(), " ")
    }

    if (override) {
        print("override ")
    }
    if (isLateinit) {
        print("lateinit ")
    }
    if (isMutable != null) {
        print(if (isMutable) "var " else "val ")
    }
    print(name, ": ", type.render())
}