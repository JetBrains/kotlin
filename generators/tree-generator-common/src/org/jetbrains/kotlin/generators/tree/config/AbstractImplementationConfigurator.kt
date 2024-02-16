/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.call

/**
 * Provides a DSL to configure `Impl` classes for tree nodes.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class AbstractImplementationConfigurator<Implementation, Element, ImplementationField>
        where Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
              Element : AbstractElement<Element, *, Implementation>,
              ImplementationField : AbstractField<*>,
              ImplementationField : AbstractFieldWithDefaultValue<*> {

    private val elementsWithImpl = mutableSetOf<Element>()

    protected abstract fun createImplementation(element: Element, name: String?): Implementation

    fun configureImplementations(model: Model<Element>) {
        configure()
        generateDefaultImplementations(model.elements)
        configureAllImplementations()
    }

    /**
     * A customization point to fine-tune existing implementation classes or add new ones.
     *
     * Override this method and use [noImpl] or [impl] in it to configure implementations of tree nodes.
     */
    protected abstract fun configure()

    /**
     * A customization point for batch-applying rules to existing implementations.
     *
     * Override this method and use [configureFieldInAllImplementations] to configure fields that are common to multiple implementation
     * classes.
     */
    protected abstract fun configureAllImplementations()

    /**
     * Disables generating any implementation classes for [element].
     */
    protected fun noImpl(element: Element) {
        element.doesNotNeedImplementation = true
    }

    /**
     * Provides a way to fine-tune a single implementation class for [element].
     *
     * @param element The element whose implementation you want to configure.
     * @param name The name of the implementation class, or `null` if you want to configure the default implementation class for this
     * element. If an implementation with this name already exists, it will be used, otherwise a new implementation will be created.
     * @param config The configuration block. See [ImplementationContext]'s documentation for description of its DSL methods.
     * @return The configured implementation.
     */
    protected fun impl(element: Element, name: String? = null, config: ImplementationContext.() -> Unit = {}): Implementation {
        val implementation = if (name == null) {
            element.defaultImplementation
        } else {
            element.customImplementations.firstOrNull { it.name == name }
        } ?: createImplementation(element, name)
        val context = ImplementationContext(implementation)
        context.apply(config)
        elementsWithImpl += element
        return implementation
    }

    private fun generateDefaultImplementations(elements: List<Element>) {
        collectLeafsWithoutImplementation(elements).forEach {
            impl(it)
        }
    }

    private fun collectLeafsWithoutImplementation(elements: List<Element>): Set<Element> {
        val leafs = elements.toMutableSet()
        elements.forEach { element ->
            leafs.removeAll(element.elementParents.map { it.element }.toSet())
        }
        leafs.removeAll(elementsWithImpl)
        return leafs
    }

    /**
     * Allows to batch-apply [config] to certain fields in _all_ the implementations that satisfy the given
     * [implementationPredicate].
     *
     * @param field The name of the field to configure across all `Impl` classes.
     * @param implementationPredicate Only implementations satisfying this predicate will be used in this configuration.
     * @param fieldPredicate Only fields satisfying this predicate will be configured
     * @param config The configuration block. Accepts the field name as an argument.
     * See [ImplementationContext]'s documentation for description of its DSL methods.
     */
    protected fun configureFieldInAllImplementations(
        field: String,
        implementationPredicate: (Implementation) -> Boolean = { true },
        fieldPredicate: (ImplementationField) -> Boolean = { true },
        config: ImplementationContext.(field: String) -> Unit,
    ) {
        for (element in elementsWithImpl) {
            for (implementation in element.allImplementations) {
                if (!implementationPredicate(implementation)) continue
                if (!implementation.allFields.any { it.name == field }) continue
                if (!fieldPredicate(implementation.getField(field))) continue
                ImplementationContext(implementation).config(field)
            }
        }
    }

    private fun Implementation.getField(name: String): ImplementationField {
        val result = this[name]
        requireNotNull(result) {
            "Field \"$name\" not found in fields of $element\nExisting fields:\n" +
                    allFields.joinToString(separator = "\n  ", prefix = "  ") { it.name }
        }
        return result
    }

    /**
     * A DSL for configuring one or more implementation classes.
     */
    protected inner class ImplementationContext(val implementation: Implementation) {

        /**
         * Call this function if you want this implementation class to be marked with an [OptIn] annotation.
         *
         * This is necessary if some code inside the implementation class requires that [OptIn] annotation.
         */
        fun optInToInternals() {
            implementation.requiresOptIn = true
        }

        /**
         * By default, all implementation classes are generated with `internal` visibility.
         *
         * This method allows to forcibly make this implementation `public`.
         */
        fun publicImplementation() {
            implementation.isPublic = true
        }

        private fun getField(name: String): ImplementationField {
            return implementation.getField(name)
        }

        /**
         * Types/functions that you want to additionally import in the file with the implementation class.
         *
         * This is useful if, for example, default values of fields reference classes or functions from other packages.
         *
         * Note that classes referenced in field types will be imported automatically.
         */
        fun additionalImports(vararg importables: Importable) {
            implementation.additionalImports.addAll(importables)
        }

        /**
         * Makes the specified fields in the implementation class mutable
         * (even if they were not configured as mutable in the element configurator).
         */
        fun isMutable(vararg fields: String) {
            fields.forEach {
                val field = getField(it)
                field.isMutable = true
            }
        }

        /**
         * Makes the specified fields in the implementation class `lateinit`
         * (even if they were not configured as `lateinit` in the element configurator).
         */
        fun isLateinit(vararg fields: String) {
            fields.forEach {
                val field = getField(it)
                field.isLateinit = true
            }
        }

        /**
         * Specifies the default value of [field] in this implementation class. The default value can be arbitrary code.
         *
         * Use [additionalImports] if the default value uses types/functions that are not otherwise imported.
         */
        fun default(field: String, value: String, withGetter: Boolean = false) {
            default(field) {
                this.value = value
                this.withGetter = withGetter
            }
        }

        /**
         * Specifies that the default value of each field of [fields] in this implementation class should be `true`.
         *
         * If [withGetter] is `true`, the fields will be generated as getter-only computed properties with their getter returning `true`,
         * otherwise, as stored properties initialized to `true`.
         */
        fun defaultTrue(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "true"
                    this.withGetter = withGetter
                }
            }
        }

        /**
         * Specifies that the default value of each field of [fields] in this implementation class should be `false`.
         *
         * If [withGetter] is `true`, the fields will be generated as getter-only computed properties with their getter returning `false`,
         * otherwise, as stored properties initialized to `false`.
         */
        fun defaultFalse(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "false"
                    this.withGetter = withGetter
                }
            }
        }

        /**
         * Specifies that the default value of each field of [fields] in this implementation class should be `null`.
         *
         * If [withGetter] is `true`, the fields will be generated as getter-only computed properties with their getter returning `null`,
         * otherwise, as stored properties initialized to `null`.
         */
        fun defaultNull(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "null"
                    this.withGetter = withGetter
                }
                require(getField(field).nullable) {
                    "$field is not nullable field"
                }
            }
        }

        /**
         * Specifies that the default value of each field of [fields] in this implementation class should be [emptyList].
         *
         * Always forces generation of a getter-only computed property.
         */
        fun defaultEmptyList(vararg fields: String) {
            for (field in fields) {
                require(getField(field).origin is ListField) {
                    "$field is list field"
                }
                default(field) {
                    value = "emptyList()"
                    withGetter = true
                }
            }
        }

        /**
         * Allows to configure the default value of [field] in this implementation class.
         *
         * See the [DefaultValueContext] documentation for description of its DSL methods.
         */
        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        /**
         * Specifies that for each field in the [fields] list its getter should be delegated to the [delegate]'s property of the same name.
         *
         * For example, `delegateFields(listOf("foo", "bar"), "myDelegate")` will result in generating the following properties in
         * the implementation class (provided that there are fields with names "foo" and "bar" in this implementation):
         * ```kotlin
         * val foo: Foo
         *     get() = myDelegate.foo
         *
         * val bar: Bar
         *     get() = myDelegate.bar
         * ```
         */
        fun delegateFields(fields: List<String>, delegate: String) {
            for (field in fields) {
                default(field) {
                    this.delegate = delegate
                }
            }
        }

        /**
         * Allows to customize this implementation class's kind (`open class`, `object` etc.).
         *
         * If set to `null`, will be chosen automatically by [InterfaceAndAbstractClassConfigurator].
         */
        var kind: ImplementationKind?
            get() = implementation.kind
            set(value) {
                implementation.kind = value
            }

        /**
         * A DSL for configuring a field's default value.
         */
        inner class DefaultValueContext(private val field: ImplementationField) {

            /**
             * The default value of this field in the implementation class. Can be arbitrary code.
             *
             * Use [additionalImports] if the default value uses types/functions that are not otherwise imported.
             */
            var value: String? = null

            /**
             * The name of the field to which to delegate this field's getter.
             *
             * For example, setting [delegate] to `"myDelegate"` will result in generating the following in
             * the implementation class (provided that we're configuring the field `foo`):
             * ```kotlin
             * val foo: Foo
             *     get() = myDelegate.foo
             * ```
             *
             * If [delegateCall] is not null, then instead of calling `foo` on `myDelegate`, the value of [delegateCall] will be used to
             * generate the call.
             */
            var delegate: String? = null
                set(value) {
                    field = value
                    if (value != null) {
                        withGetter = true
                    }
                }

            /**
             * If [delegate] is not null, allows to specify a call that will be generated on [delegate].
             *
             * For example, setting [delegate] to `"myDelegate"` and [delegateCall] to `"getFoo()"` will result in generating the following
             * in the implementation class  (provided that we're configuring the field `foo`):
             * ```kotlin
             * val foo: Foo
             *     get() = myDelegate.getFoo()
             * ```
             *
             * If [delegate] is `null`, setting this property has no effect.
             */
            var delegateCall: String? = null

            /**
             * Forces the specified mutability on this field in the implementation class.
             *
             * If set to `null`, the mutability specified in the element configurator will be used.
             */
            var isMutable: Boolean? = null

            /**
             * If `true`, the field will be generated as a computed property instead of stored one.
             */
            var withGetter: Boolean = false
                set(value) {
                    field = value
                    if (value) {
                        isMutable = customSetter != null
                    }
                }

            /**
             * Specifies the value of this field's setter.
             *
             * If set to a non-null value, the generated property is automatically made mutable and computed.
             *
             * Can be arbitrary code. Use [additionalImports] if this code uses types/functions that are not otherwise imported.
             */
            var customSetter: String? = null
                set(value) {
                    field = value
                    isMutable = true
                    withGetter = true
                }

            /**
             * Whether this field semantically represents a reference to a child node of the tree.
             *
             * This has the effect of including or excluding this field from visiting it by visitors in the generated
             * `acceptChildren` and `transformChildren` methods (child fields are always visited in those methods)
             */
            var isChild: Boolean = true

            fun applyConfiguration() {
                field.withGetter = withGetter
                field.customSetter = customSetter
                isMutable?.let { field.isMutable = it }
                field.isChild = isChild

                when {
                    value != null -> field.defaultValueInImplementation = value
                    delegate != null -> {
                        val actualDelegateField = getField(delegate!!)
                        val name = delegateCall ?: field.name
                        field.defaultValueInImplementation = "${actualDelegateField.name}${actualDelegateField.call()}$name"
                    }
                }
            }
        }
    }
}
