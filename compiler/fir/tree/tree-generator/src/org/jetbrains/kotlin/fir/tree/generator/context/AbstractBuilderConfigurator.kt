/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.noReceiverExpressionType
import org.jetbrains.kotlin.fir.tree.generator.printer.call
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class AbstractBuilderConfigurator<T : AbstractFirTreeBuilder>(val firTreeBuilder: T) {
    abstract class BuilderConfigurationContext {
        abstract val builder: Builder

        private fun getField(name: String): FieldWithDefault {
            return builder[name]
        }

        fun useTypes(vararg types: Importable) {
            types.forEach { builder.usedTypes += it }
        }

        fun defaultNoReceivers() {
            defaultNull("explicitReceiver")
            default("dispatchReceiver", "FirNoReceiverExpression")
            default("extensionReceiver", "FirNoReceiverExpression")
            useTypes(noReceiverExpressionType)
        }

        fun default(field: String, value: String) {
            default(field) {
                this.value = value
            }
        }

        fun defaultTrue(field: String) {
            default(field) {
                value = "true"
            }
        }

        fun defaultFalse(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "false"
                }
            }
        }

        fun defaultNull(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "null"
                }
                require(getField(field).nullable) {
                    "$field is not nullable field"
                }
            }
        }

        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        inner class DefaultValueContext(private val field: FieldWithDefault) {
            var value: String? = null

            fun applyConfiguration() {
                if (value != null) field.defaultValueInBuilder = value
            }
        }
    }


    class IntermediateBuilderConfigurationContext(override val builder: IntermediateBuilder) : BuilderConfigurationContext() {
        inner class Fields {
            // fields from <element>
            infix fun from(element: Element): ExceptConfigurator {
                builder.fields += element.allFields.map {
                    FieldWithDefault(it.copy())
                }
                builder.packageName = "${element.packageName}.builder"
                builder.materializedElement = element
                return ExceptConfigurator()
            }

            inner class Helper(val fieldName: String) {
                infix fun from(element: Element) {
                    val field = element[fieldName] ?: throw IllegalArgumentException("Element $element doesn't have field $fieldName")
                    builder.fields += FieldWithDefault(field)
                }
            }

            // fields has <field> from <element>
            infix fun has(name: String): Helper = Helper(name)
        }

        inner class ExceptConfigurator {
            infix fun without(name: String) {
                without(listOf(name))
            }

            infix fun without(names: List<String>) {
                builder.fields.removeAll { it.name in names }
            }
        }

        val fields = Fields()
        val parents: MutableList<IntermediateBuilder> get() = builder.parents

        var materializedElement: Element
            get() = throw IllegalArgumentException()
            set(value) {
                builder.materializedElement = value
            }

    }

    inner class IntermediateBuilderDelegateProvider(
        private val name: String?,
        private val block: IntermediateBuilderConfigurationContext.() -> Unit
    ) {
        lateinit var builder: IntermediateBuilder

        operator fun provideDelegate(
            thisRef: Nothing?,
            prop: KProperty<*>
        ): ReadOnlyProperty<Nothing?, IntermediateBuilder> {
            val name = name ?: "Fir${prop.name.capitalize()}"
            builder = IntermediateBuilder(name).apply {
                firTreeBuilder.intermediateBuilders += this
                IntermediateBuilderConfigurationContext(this).block()
            }
            return DummyDelegate(builder)
        }

        private inner class DummyDelegate(val builder: IntermediateBuilder) : ReadOnlyProperty<Nothing?, IntermediateBuilder> {
            override fun getValue(thisRef: Nothing?, property: KProperty<*>): IntermediateBuilder {
                return builder
            }
        }
    }

    inner class LeafBuilderConfigurationContext(override val builder: LeafBuilder) : BuilderConfigurationContext() {
        val parents: MutableList<IntermediateBuilder> get() = builder.parents

        fun openBuilder() {
            builder.isOpen = true
        }
    }

    fun builder(name: String? = null, block: IntermediateBuilderConfigurationContext.() -> Unit): IntermediateBuilderDelegateProvider {
        return IntermediateBuilderDelegateProvider(name, block)
    }

    fun builder(element: Element, type: String? = null, init: LeafBuilderConfigurationContext.() -> Unit) {
        val implementation = element.extractImplementation(type)
        val builder = implementation.builder
        requireNotNull(builder)
        LeafBuilderConfigurationContext(builder).apply(init)
    }

    private fun Element.extractImplementation(type: String?): Implementation {
        return if (type == null) {
            allImplementations.filter { it.kind?.hasLeafBuilder == true }.singleOrNull() ?: this@AbstractBuilderConfigurator.run {
                val message = buildString {
                    appendln("${this@extractImplementation} has multiple implementations:")
                    for (implementation in allImplementations) {
                        appendln("  - ${implementation.type}")
                    }
                    appendln("Please specify implementation is needed")
                }
                throw IllegalArgumentException(message)
            }
        } else {
            allImplementations.firstOrNull { it.type == type } ?: this@AbstractBuilderConfigurator.run {
                val message = buildString {
                    appendln("${this@extractImplementation} has not implementation $type. Existing implementations:")
                    for (implementation in allImplementations) {
                        appendln("  - ${implementation.type}")
                    }
                    appendln("Please specify implementation is needed")
                }
                throw IllegalArgumentException(message)
            }
        }
    }

    fun noBuilder(element: Element, type: String? = null) {
        val implementation = element.extractImplementation(type)
        implementation.builder = null
    }
}