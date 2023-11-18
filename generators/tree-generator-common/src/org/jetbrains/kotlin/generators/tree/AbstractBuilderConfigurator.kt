/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.utils.DummyDelegate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Allows to fine-tune the logic of builder generation, for example, add intermediate builders, or add default values
 * for properties in the generated builders.
 */
abstract class AbstractBuilderConfigurator<Element, Implementation, BuilderField, ElementField>(
    val elements: List<Element>,
) where Element : AbstractElement<Element, ElementField, Implementation>,
        Implementation : AbstractImplementation<Implementation, Element, BuilderField>,
        BuilderField : AbstractField<*>,
        BuilderField : AbstractFieldWithDefaultValue<*>,
        ElementField : AbstractField<ElementField> {

    protected abstract val namePrefix: String

    protected abstract val defaultBuilderPackage: String

    protected abstract fun builderFieldFromElementField(elementField: ElementField): BuilderField

    val intermediateBuilders = mutableListOf<IntermediateBuilder<BuilderField, Element>>()

    fun builder(name: String? = null, block: IntermediateBuilderConfigurationContext.() -> Unit): IntermediateBuilderDelegateProvider {
        return IntermediateBuilderDelegateProvider(name, block)
    }

    fun builder(element: Element, type: String? = null, init: LeafBuilderConfigurationContext.() -> Unit) {
        val implementation = element.extractImplementation(type)
        val builder = implementation.builder
        requireNotNull(builder)
        LeafBuilderConfigurationContext(builder).apply(init)
    }

    fun noBuilder(element: Element, type: String? = null) {
        val implementation = element.extractImplementation(type)
        implementation.builder = null
    }

    private fun Element.extractImplementation(type: String?): Implementation {
        return if (type == null) {
            allImplementations.singleOrNull { it.kind?.hasLeafBuilder == true } ?: this@AbstractBuilderConfigurator.run {
                val message = buildString {
                    appendLine("${this@extractImplementation} has multiple implementations:")
                    for (implementation in allImplementations) {
                        appendLine("  - ${implementation.typeName}")
                    }
                    appendLine("Please specify implementation is needed")
                }
                throw IllegalArgumentException(message)
            }
        } else {
            allImplementations.firstOrNull { it.typeName == type } ?: this@AbstractBuilderConfigurator.run {
                val message = buildString {
                    appendLine("${this@extractImplementation} has not implementation $type. Existing implementations:")
                    for (implementation in allImplementations) {
                        appendLine("  - ${implementation.typeName}")
                    }
                    appendLine("Please specify implementation is needed")
                }
                throw IllegalArgumentException(message)
            }
        }
    }

    protected inline fun findImplementationsWithElementInParents(
        element: Element,
        implementationPredicate: (Implementation) -> Boolean = { true }
    ): Collection<Implementation> {
        return elements.flatMap { it.allImplementations }.mapNotNullTo(mutableSetOf()) {
            if (!implementationPredicate(it)) return@mapNotNullTo null
            var hasAnnotations = false
            if (it.element == element) return@mapNotNullTo null
            it.element.traverseParents {
                if (it == element) {
                    hasAnnotations = true
                }
            }
            it.takeIf { hasAnnotations }
        }
    }

    protected fun configureFieldInAllLeafBuilders(
        field: String,
        builderPredicate: ((LeafBuilder<BuilderField, Element, Implementation>) -> Boolean)? = null,
        fieldPredicate: ((BuilderField) -> Boolean)? = null,
        init: LeafBuilderConfigurationContext.(field: String) -> Unit
    ) {
        val builders = elements.flatMap { it.allImplementations }.mapNotNull { it.builder }
        for (builder in builders) {
            if (builderPredicate != null && !builderPredicate(builder)) continue
            if (!builder.allFields.any { it.name == field }) continue
            if (fieldPredicate != null && !fieldPredicate(builder[field])) continue
            LeafBuilderConfigurationContext(builder).init(field)
        }
    }

    abstract inner class BuilderConfigurationContext {
        abstract val builder: Builder<BuilderField, Element>

        private fun getField(name: String): BuilderField {
            return builder[name]
        }

        fun useTypes(vararg types: Importable) {
            types.forEach { builder.usedTypes += it }
        }

        fun defaultNoReceivers(notNullExplicitReceiver: Boolean = false) {
            if (!notNullExplicitReceiver) {
                defaultNull("explicitReceiver")
            }
            defaultNull("dispatchReceiver", "extensionReceiver")
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

        inner class DefaultValueContext(private val field: BuilderField) {
            var value: String? = null
            var notNull: Boolean? = null

            fun applyConfiguration() {
                if (value != null) field.defaultValueInBuilder = value
                if (notNull != null) field.notNull = notNull!!
            }
        }
    }

    inner class IntermediateBuilderConfigurationContext(
        override val builder: IntermediateBuilder<BuilderField, Element>
    ) : BuilderConfigurationContext() {
        inner class Fields {
            // fields from <element>
            infix fun from(element: Element): ExceptConfigurator {
                builder.fields += element.allFields.map(this@AbstractBuilderConfigurator::builderFieldFromElementField)
                builder.packageName = "${element.packageName}.builder"
                builder.materializedElement = element
                return ExceptConfigurator()
            }
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

        val parents: MutableList<IntermediateBuilder<BuilderField, Element>>
            get() = builder.parents
    }

    inner class IntermediateBuilderDelegateProvider(
        private val name: String?,
        private val block: IntermediateBuilderConfigurationContext.() -> Unit
    ) {
        lateinit var builder: IntermediateBuilder<BuilderField, Element>

        operator fun provideDelegate(
            thisRef: Nothing?,
            prop: KProperty<*>
        ): ReadOnlyProperty<Nothing?, IntermediateBuilder<BuilderField, Element>> {
            val name = name ?: (namePrefix + prop.name.replaceFirstChar(Char::uppercaseChar))
            builder = IntermediateBuilder<BuilderField, Element>(name, defaultBuilderPackage).apply {
                intermediateBuilders += this
                IntermediateBuilderConfigurationContext(this).block()
            }
            return DummyDelegate(builder)
        }
    }

    inner class LeafBuilderConfigurationContext(
        override val builder: LeafBuilder<BuilderField, Element, Implementation>
    ) : BuilderConfigurationContext() {
        val parents: MutableList<IntermediateBuilder<BuilderField, Element>>
            get() = builder.parents

        fun openBuilder() {
            builder.isOpen = true
        }

        fun withCopy() {
            builder.wantsCopy = true
        }
    }
}