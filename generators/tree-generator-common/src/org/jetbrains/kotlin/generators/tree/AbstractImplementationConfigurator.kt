/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.printer.call

abstract class AbstractImplementationConfigurator<Implementation, Element, ImplementationField>
        where Implementation : AbstractImplementation<Implementation, Element, ImplementationField>,
              Element : AbstractElement<Element, *, Implementation>,
              ImplementationField : AbstractField<*>,
              ImplementationField : AbstractFieldWithDefaultValue<*> {

    private val elementsWithImpl = mutableSetOf<Element>()

    protected abstract fun createImplementation(element: Element, name: String?): Implementation

    protected fun noImpl(element: Element) {
        element.doesNotNeedImplementation = true
    }

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

    protected fun generateDefaultImplementations(elements: List<Element>) {
        collectLeafsWithoutImplementation(elements).forEach {
            impl(it)
        }
    }

    private fun collectLeafsWithoutImplementation(elements: List<Element>): Set<Element> {
        val leafs = elements.toMutableSet()
        elements.forEach {
            leafs.removeAll(it.elementParents.map { it.element }.toSet())
        }
        leafs.removeAll(elementsWithImpl)
        return leafs
    }

    protected fun configureFieldInAllImplementations(
        field: String,
        implementationPredicate: ((Implementation) -> Boolean)? = null,
        fieldPredicate: ((ImplementationField) -> Boolean)? = null,
        init: ImplementationContext.(field: String) -> Unit
    ) {
        for (element in elementsWithImpl) {
            for (implementation in element.allImplementations) {
                if (implementationPredicate != null && !implementationPredicate(implementation)) continue
                if (!implementation.allFields.any { it.name == field }) continue
                if (fieldPredicate != null && !fieldPredicate(implementation.getField(field))) continue
                ImplementationContext(implementation).init(field)
            }
        }
    }

    private fun Implementation.getField(name: String): ImplementationField {
        val result = allFields.firstOrNull { it.name == name }
        requireNotNull(result) {
            "Field \"$name\" not found in fields of ${element}\nExisting fields:\n" +
                    allFields.joinToString(separator = "\n  ", prefix = "  ") { it.name }
        }
        return result
    }

    protected inner class ImplementationContext(val implementation: Implementation) {

        fun optInToInternals() {
            implementation.requiresOptIn = true
        }

        fun publicImplementation() {
            implementation.isPublic = true
        }

        private fun getField(name: String): ImplementationField {
            return implementation.getField(name)
        }

        fun useTypes(vararg types: Importable) {
            types.forEach { implementation.usedTypes += it }
        }

        fun isMutable(vararg fields: String) {
            fields.forEach {
                val field = getField(it)
                field.isMutable = true
            }
        }

        fun isLateinit(vararg fields: String) {
            fields.forEach {
                val field = getField(it)
                field.isLateinit = true
            }
        }

        fun default(field: String, value: String) {
            default(field) {
                this.value = value
            }
        }

        fun defaultTrue(field: String, withGetter: Boolean = false) {
            default(field) {
                value = "true"
                this.withGetter = withGetter
            }
        }

        fun defaultFalse(vararg fields: String, withGetter: Boolean = false) {
            for (field in fields) {
                default(field) {
                    value = "false"
                    this.withGetter = withGetter
                }
            }
        }

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

        fun defaultEmptyList(field: String) {
            require(getField(field).origin is ListField) {
                "$field is list field"
            }
            default(field) {
                value = "emptyList()"
                withGetter = true
            }
        }

        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        fun delegateFields(fields: List<String>, delegate: String) {
            for (field in fields) {
                default(field) {
                    this.delegate = delegate
                }
            }
        }

        var kind: ImplementationKind?
            get() = implementation.kind
            set(value) {
                implementation.kind = value
            }

        inner class DefaultValueContext(private val field: ImplementationField) {
            var value: String? = null

            var delegate: String? = null
                set(value) {
                    field = value
                    if (value != null) {
                        withGetter = true
                    }
                }

            var delegateCall: String? = null

            var isMutable: Boolean? = null

            var withGetter: Boolean = false
                set(value) {
                    field = value
                    if (value) {
                        isMutable = customSetter != null
                    }
                }

            var customSetter: String? = null
                set(value) {
                    field = value
                    isMutable = true
                    withGetter = true
                }

            var needAcceptAndTransform: Boolean = true

            var notNull: Boolean = false

            fun applyConfiguration() {
                field.withGetter = withGetter
                field.customSetter = customSetter
                isMutable?.let { field.isMutable = it }
                field.needAcceptAndTransform = needAcceptAndTransform

                if (notNull) {
                    field.notNull = true
                }
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