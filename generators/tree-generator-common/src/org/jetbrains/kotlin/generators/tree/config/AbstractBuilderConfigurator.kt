/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.config.AbstractImplementationConfigurator.ImplementationContext.DefaultValueContext
import org.jetbrains.kotlin.utils.DummyDelegate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Provides a DSL to configure builder classes for tree nodes, for example, add intermediate builders, or add default values
 * for properties in the generated builders.
 */
abstract class AbstractBuilderConfigurator<Element, Implementation, BuilderField, ElementField>(
    val elements: List<Element>,
) where Element : AbstractElement<Element, ElementField, Implementation>,
        Implementation : AbstractImplementation<Implementation, Element, BuilderField>,
        BuilderField : AbstractField<*>,
        BuilderField : AbstractFieldWithDefaultValue<*>,
        ElementField : AbstractField<ElementField> {

    /**
     * The prefix that will be used to generate names of builder classes.
     *
     * Should be the same as [AbstractElement.namePrefix].
     */
    protected abstract val namePrefix: String

    /**
     * The package in which [IntermediateBuilder]s should be generated.
     */
    protected abstract val defaultBuilderPackage: String

    /**
     * A customization point to fine-tune existing builder classes or add new ones.
     *
     * Override this method and use the following DSL methods to configure builder generation:
     * - [builder]
     * - [noBuilder]
     * - [configureFieldInAllLeafBuilders]
     */
    abstract fun configureBuilders()

    /**
     * Must return a copy of [elementField] that will be used in builder configuration.
     */
    protected abstract fun builderFieldFromElementField(elementField: ElementField): BuilderField

    val intermediateBuilders = mutableListOf<IntermediateBuilder<BuilderField, Element>>()

    /**
     * Provides a way to configure an intermediate builder class.
     *
     * @param config The configuration block. See [IntermediateBuilderConfigurationContext]'s documentation for description of its DSL
     * methods.
     */
    protected fun builder(config: IntermediateBuilderConfigurationContext.() -> Unit) = IntermediateBuilderDelegateProvider(config)

    /**
     * Provides a way to configure a leaf builder class, i.e. the builder class responsible for finally constructing an instance of
     * the corresponding implementation class.
     *
     * @param element The element for which to configure builder generation.
     * @param config The configuration block. See [LeafBuilderConfigurationContext]'s documentation for description of its DSL
     * methods.
     */
    protected fun builder(element: Element, type: String? = null, config: LeafBuilderConfigurationContext.() -> Unit) {
        val implementation = element.extractImplementation(type)
        val builder = implementation.builder
        requireNotNull(builder)
        LeafBuilderConfigurationContext(builder).apply(config)
    }

    /**
     * Disables generating any builder classes for [element].
     */
    protected fun noBuilder(element: Element, type: String? = null) {
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

    /**
     * Out of all implementations, returns those for which [implementationPredicate] returns `true`
     * _and_ [element] is one of its non-immediate parents.
     */
    protected inline fun findImplementationsWithElementInParents(
        element: Element,
        implementationPredicate: (Implementation) -> Boolean = { true }
    ): Collection<Implementation> {
        return elements
            .flatMap { it.allImplementations }
            .mapNotNullTo(mutableSetOf()) { implementation ->
                if (!implementationPredicate(implementation)) return@mapNotNullTo null
                if (implementation.element == element) return@mapNotNullTo null
                val hasElementInParents = implementation.element.traverseParentsUntil { it == element }
                implementation.takeIf { hasElementInParents }
            }
    }

    private val allLeafBuilders: List<LeafBuilder<BuilderField, Element, Implementation>>
        get() = elements.flatMap { it.allImplementations }.mapNotNull { it.builder }

    /**
     * Allows to batch-apply [config] to certain fields in _all_ the builders that satisfy the given
     * [builderPredicate].
     *
     * @param field The name of the field to configure across all builder classes.
     * @param builderPredicate Only builders satisfying this predicate will participate in this configuration.
     * @param fieldPredicate Only fields satisfying this predicate will be configured.
     * @param config The configuration block. Accepts the field name as an argument.
     * See [LeafBuilderConfigurationContext]'s documentation for description of its DSL methods.
     */
    protected fun configureFieldInAllLeafBuilders(
        field: String,
        builderPredicate: ((LeafBuilder<BuilderField, Element, Implementation>) -> Boolean)? = null,
        fieldPredicate: ((BuilderField) -> Boolean)? = null,
        config: LeafBuilderConfigurationContext.(field: String) -> Unit
    ) {
        for (builder in allLeafBuilders) {
            if (builderPredicate != null && !builderPredicate(builder)) continue
            if (!builder.allFields.any { it.name == field }) continue
            if (fieldPredicate != null && !fieldPredicate(builder[field])) continue
            LeafBuilderConfigurationContext(builder).config(field)
        }
    }

    /**
     * Allows to batch-apply [config] to _all_ leaf builders.
     *
     * @param config The configuration block. See [LeafBuilderConfigurationContext]'s documentation for description of its DSL methods.
     */
    protected fun configureAllLeafBuilders(config: LeafBuilderConfigurationContext.() -> Unit) {
        for (builder in allLeafBuilders) {
            LeafBuilderConfigurationContext(builder).config()
        }
    }

    /**
     * A DSL for configuring one or more intermediate or leaf builder classes.
     */
    protected abstract inner class BuilderConfigurationContext {
        protected abstract val builder: Builder<BuilderField, Element>

        private fun getField(name: String): BuilderField {
            return builder[name]
        }

        /**
         * Types/functions that you want to additionally import in the file with the builder class.
         *
         * This is useful if, for example, default values of fields reference classes or functions from other packages.
         *
         * Note that classes referenced in field types will be imported automatically.
         */
        fun additionalImports(vararg types: Importable) {
            types.forEach { builder.usedTypes += it }
        }

        /**
         * Specifies the default value of [field] in this builder class. The default value can be arbitrary code.
         *
         * Use [additionalImports] if the default value uses types/functions that are not otherwise imported.
         */
        fun default(field: String, value: String) {
            default(field) {
                this.value = value
            }
        }

        /**
         * Specifies that the default value of each field in [fields] in this builder class should be `true`.
         */
        fun defaultTrue(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "true"
                }
            }
        }

        /**
         * Specifies that the default value of each field in [fields] in this builder class should be `false`.
         */
        fun defaultFalse(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "false"
                }
            }
        }

        /**
         * Specifies that the default value of each field of [fields] in this builder class should be `null`.
         *
         * Note: the field must be configured as nullable.
         */
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

        /**
         * Allows to configure the default value of [field] in this builder class.
         *
         * See the [DefaultValueContext] documentation for description of its DSL methods.
         */
        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        /**
         * A DSL for configuring a field's default value.
         */
        inner class DefaultValueContext(private val field: BuilderField) {

            /**
             * The default value of this field in the builder class. Can be arbitrary code.
             *
             * Use [additionalImports] if the default value uses types/functions that are not otherwise imported.
             */
            var value: String? = null

            fun applyConfiguration() {
                if (value != null) field.defaultValueInBuilder = value
            }
        }
    }

    /**
     * A DSL for configuring one or more intermediate builder classes.
     *
     * Use the following syntax for configuring the set of generated fields in this builder class:
     * ```kotlin
     * fields from myElement // To use all fields from myElement in the builder class
     * fields from myElement without "myField" // To use all fields except myField from myElement in the builder class.
     * fields from myElement without listOf("foo", "bar") // To use all fields except foo and bar from myElement in the builder class.
     * ```
     */
    protected inner class IntermediateBuilderConfigurationContext(
        override val builder: IntermediateBuilder<BuilderField, Element>
    ) : BuilderConfigurationContext() {
        inner class Fields {

            /**
             * Copy all fields from [element] to this builder class.
             */
            infix fun from(element: Element): ExceptConfigurator {
                builder.fields += element.allFields.map(this@AbstractBuilderConfigurator::builderFieldFromElementField)
                builder.packageName = "${element.packageName}.builder"
                builder.materializedElement = element
                return ExceptConfigurator()
            }
        }

        inner class ExceptConfigurator {

            /**
             * Exclude the field with [name] from this builder class.
             */
            infix fun without(name: String) {
                without(listOf(name))
            }

            /**
             * Exclude the fields with [names] from this builder class.
             */
            infix fun without(names: List<String>) {
                builder.fields.removeAll { it.name in names }
            }
        }

        /**
         * A configurator for copying fields from some element to this intermediate builder.
         *
         * See [IntermediateBuilderConfigurationContext] for the usage example.
         */
        val fields = Fields()

        /**
         * The list of parents of this intermediate builder. Can be used for adding builder superclasses to this builder class.
         */
        val parents: MutableList<IntermediateBuilder<BuilderField, Element>>
            get() = builder.parents
    }

    protected inner class IntermediateBuilderDelegateProvider(
        private val block: IntermediateBuilderConfigurationContext.() -> Unit
    ) {
        lateinit var builder: IntermediateBuilder<BuilderField, Element>

        operator fun provideDelegate(
            thisRef: Nothing?,
            prop: KProperty<*>
        ): ReadOnlyProperty<Nothing?, IntermediateBuilder<BuilderField, Element>> {
            val name = namePrefix + prop.name.replaceFirstChar(Char::uppercaseChar)
            builder = IntermediateBuilder<BuilderField, Element>(name, defaultBuilderPackage).apply {
                intermediateBuilders += this
                IntermediateBuilderConfigurationContext(this).block()
            }
            return DummyDelegate(builder)
        }
    }

    /**
     * A DSL for configuring one or more leaf builder classes.
     */
    protected inner class LeafBuilderConfigurationContext(
        override val builder: LeafBuilder<BuilderField, Element, Implementation>
    ) : BuilderConfigurationContext() {

        /**
         * The list of parents of this leaf builder. Can be used for adding builder superclasses to this builder class.
         */
        val parents: MutableList<IntermediateBuilder<BuilderField, Element>>
            get() = builder.parents

        /**
         * Makes this builder an open class.
         */
        fun openBuilder() {
            builder.isOpen = true
        }

        /**
         * In addition to the regular `build*()` function, generate `build*Copy()` function that accepts
         * an instance of the corresponding tree element and copies values from that instance to the builder, allowing to change them
         * in the process.
         */
        fun withCopy() {
            builder.wantsCopy = true
        }
    }
}
