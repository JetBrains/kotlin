/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.config

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.Importable
import org.jetbrains.kotlin.generators.tree.printer.call

/**
 * Provides a DSL to configure `Impl` classes for tree nodes.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class AbstractImplementationConfigurator<Implementation, Element, ElementField, ImplementationField>
        where Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
              Element : AbstractElement<Element, ElementField, Implementation>,
              ElementField : AbstractField<ElementField>,
              ImplementationField : AbstractField<*> {

    private val elementsWithImpl = mutableSetOf<Element>()

    protected abstract fun createImplementation(element: Element, name: String?): Implementation

    fun configureImplementations(model: Model<Element>) {
        configure(model)
        generateDefaultImplementations(model.elements)
        inheritImplementationFieldSpecifications(model.elements)
        configureAllImplementations(model)
    }

    /**
     * A customization point to fine-tune existing implementation classes or add new ones.
     *
     * Override this method and use [noImpl] or [impl] in it to configure implementations of tree nodes.
     */
    protected abstract fun configure(model: Model<Element>)

    /**
     * A customization point for batch-applying rules to existing implementations.
     *
     * Override this method and use [configureFieldInAllImplementations] to configure fields that are common to multiple implementation
     * classes.
     */
    protected abstract fun configureAllImplementations(model: Model<Element>)

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
        val implementation = element.implementations.firstOrNull { it.name == name }
            ?: createImplementation(element, name)
        val context = ImplementationContext(implementation)
        context.apply(config)
        elementsWithImpl += element
        return implementation
    }

    /**
     * Provides a way to fine-tune all implementations of classes deriving from [element].
     */
    protected fun allImplOf(element: Element, config: ElementContext.() -> Unit) {
        val context = ElementContext(element)
        context.apply(config)
    }

    private fun generateDefaultImplementations(elements: List<Element>) {
        elements
            .filter { it.subElements.isEmpty() && it !in elementsWithImpl && !it.doesNotNeedImplementation }
            .forEach {
                impl(it)
            }
    }

    /**
     * Apply the configuration done in [allImplOf] to all actual implementation classes, choosing the
     * most specific configuration for a given implementation, or applies default value if no
     * customized configuration is found.
     */
    private fun inheritImplementationFieldSpecifications(elements: List<Element>) {
        for (element in elements) {
            for (implementation in element.implementations) {
                for (field in implementation.allFields) {
                    if (field.implementationDefaultStrategy == null) {
                        for (ancestor in element.elementAncestorsAndSelfBreadthFirst()) {
                            val inheritedDefaults = ancestor.elementParents
                                .mapNotNull { it.element.getOrNull(field.name) }
                                .mapNotNull { it.implementationDefaultStrategy }
                            if (inheritedDefaults.isNotEmpty()) {
                                field.implementationDefaultStrategy = inheritedDefaults.singleOrNull()
                                    ?: error("Field $field has ambiguous default value, please specify it explicitly for the ${element.name} element")
                                break
                            }
                        }

                        if (field.implementationDefaultStrategy == null) {
                            field.implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Required
                        }
                    }
                }
            }
        }
    }


    /**
     * Allows to batch-apply [config] to certain fields in _all_ the implementations that satisfy the given
     * [implementationPredicate].
     *
     * @param fieldName The name of the field to configure across all `Impl` classes, or `null` if [config] should be applied to all fields.
     * @param implementationPredicate Only implementations satisfying this predicate will be used in this configuration.
     * @param fieldPredicate Only fields satisfying this predicate will be configured
     * @param config The configuration block. Accepts the field name as an argument.
     * See [ImplementationContext]'s documentation for description of its DSL methods.
     */
    protected fun configureFieldInAllImplementations(
        fieldName: String?,
        implementationPredicate: (Implementation) -> Boolean = { true },
        fieldPredicate: (ImplementationField) -> Boolean = { true },
        config: ImplementationContext.(field: String) -> Unit,
    ) {
        for (element in elementsWithImpl) {
            for (implementation in element.implementations) {
                if (!implementationPredicate(implementation)) continue
                if (fieldName != null && !implementation.allFields.any { it.name == fieldName }) continue

                val fields = if (fieldName != null) {
                    listOf(implementation[fieldName])
                } else {
                    implementation.allFields
                }

                for (field in fields.filter(fieldPredicate)) {
                    ImplementationContext(implementation).config(field.name)
                }
            }
        }
    }

    /**
     * Allows to batch-apply [config] to _all_ the implementations that satisfy the given
     * [implementationPredicate].
     *
     * @param implementationPredicate Only implementations satisfying this predicate will be used in this configuration.
     * @param config The configuration block. Accepts the field name as an argument.
     * See [ImplementationContext]'s documentation for description of its DSL methods.
     */
    protected fun configureAllImplementations(
        implementationPredicate: (Implementation) -> Boolean = { true },
        config: ImplementationContext.() -> Unit,
    ) {
        for (element in elementsWithImpl) {
            for (implementation in element.implementations) {
                if (!implementationPredicate(implementation)) continue
                ImplementationContext(implementation).config()
            }
        }
    }

    protected abstract class FieldContainerContext<Field>(
        private val fieldContainer: FieldContainer<Field>,
    ) where Field : AbstractField<*> {
        /**
         * Makes the specified fields in the implementation class mutable
         * (even if they were not configured as mutable in the element configurator).
         */
        fun isMutable(vararg fields: String) {
            fields.forEach {
                val field = fieldContainer[it]
                field.isMutable = true
            }
        }

        /**
         * Makes the specified fields in the implementation class `lateinit`
         * (even if they were not configured as `lateinit` in the element configurator).
         */
        fun isLateinit(vararg fields: String) {
            fields.forEach {
                val field = fieldContainer[it]
                field.implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Lateinit
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
                require(fieldContainer[field].nullable) {
                    "$field is not nullable field"
                }
            }
        }

        /**
         * Specifies that the default value of each field of [fields] in this implementation class should be [emptyList].
         *
         * @param withGetter If `true`, the field will be generated as a computed property instead of stored one.
         */
        fun defaultEmptyList(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                require(fieldContainer[field].origin is ListField) {
                    "$field is list field"
                }
                default(field) {
                    value = "emptyList()"
                    this.withGetter = withGetter
                }
            }
        }

        /**
         * Allows to configure the default value of [field] in this implementation class.
         *
         * See the [DefaultValueContext] documentation for description of its DSL methods.
         */
        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(fieldContainer[field]).apply(init).applyConfiguration()
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
         * A DSL for configuring a field's default value.
         */
        inner class DefaultValueContext(private val field: Field) {

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

            fun applyConfiguration() {
                field.customSetter = customSetter
                isMutable?.let { field.isMutable = it }

                var value = value
                when {
                    value != null -> field.implementationDefaultStrategy =
                        AbstractField.ImplementationDefaultStrategy.DefaultValue(value, withGetter)
                    delegate != null -> {
                        val actualDelegateField = fieldContainer[delegate!!]
                        val name = delegateCall ?: field.name
                        value = "${actualDelegateField.name}${actualDelegateField.call()}$name"
                        field.implementationDefaultStrategy =
                            AbstractField.ImplementationDefaultStrategy.DefaultValue(value, withGetter)
                    }
                }
            }
        }
    }

    /**
     * A DSL for configuring one or more implementation classes.
     */
    protected inner class ImplementationContext(val implementation: Implementation) :
        FieldContainerContext<ImplementationField>(implementation) {
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
         * Allows to customize this implementation class's kind (`open class`, `object` etc.).
         *
         * If set to `null`, will be chosen automatically by [InterfaceAndAbstractClassConfigurator].
         */
        var kind: ImplementationKind?
            get() = implementation.kind
            set(value) {
                implementation.kind = value
            }

        fun kDoc(kDoc: String) {
            implementation.kDoc = kDoc
        }
    }

    /**
     * A DSL for configuring implementations of all classes extending given element.
     */
    protected inner class ElementContext(val element: Element) : FieldContainerContext<ElementField>(element)
}
