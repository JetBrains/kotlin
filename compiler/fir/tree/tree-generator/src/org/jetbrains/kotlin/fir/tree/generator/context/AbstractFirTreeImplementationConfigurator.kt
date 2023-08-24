/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.context

import org.jetbrains.kotlin.fir.tree.generator.constructClassLikeTypeImport
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.noReceiverExpressionType
import org.jetbrains.kotlin.fir.tree.generator.printer.call
import org.jetbrains.kotlin.fir.tree.generator.standardClassIdsType

abstract class AbstractFirTreeImplementationConfigurator {
    private val elementsWithImpl = mutableSetOf<Element>()

    fun noImpl(element: Element) {
        element.doesNotNeedImplementation = true
    }

    fun impl(element: Element, name: String? = null, config: ImplementationContext.() -> Unit = {}): Implementation {
        val implementation = if (name == null) {
            element.defaultImplementation
        } else {
            element.customImplementations.firstOrNull { it.name == name }
        } ?: Implementation(element, name)
        val context = ImplementationContext(implementation)
        context.apply(config)
        implementation.updateMutabilityAccordingParents()
        elementsWithImpl += element
        return implementation
    }

    protected fun generateDefaultImplementations(builder: AbstractFirTreeBuilder) {
        collectLeafsWithoutImplementation(builder).forEach {
            impl(it)
        }
    }

    protected fun configureFieldInAllImplementations(
        field: String,
        implementationPredicate: ((Implementation) -> Boolean)? = null,
        fieldPredicate: ((Field) -> Boolean)? = null,
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

    private fun collectLeafsWithoutImplementation(builder: AbstractFirTreeBuilder): Set<Element> {
        val elements = builder.elements.toMutableSet()
        builder.elements.forEach {
            elements.removeAll(it.parents)
        }
        elements.removeAll(elementsWithImpl)
        return elements
    }

    private fun Implementation.getField(name: String): FieldWithDefault {
        val result = allFields.firstOrNull { it.name == name }
        requireNotNull(result) {
            "Field \"$name\" not found in fields of ${element}\nExisting fields:\n" +
                    allFields.joinToString(separator = "\n  ", prefix = "  ") { it.name }
        }
        return result
    }

    inner class ImplementationContext(private val implementation: Implementation) {
        private fun getField(name: String): FieldWithDefault {
            return implementation.getField(name)
        }

        inner class ParentsHolder {
            operator fun plusAssign(parent: Implementation) {
                implementation.addParent(parent)
            }

            operator fun plusAssign(parent: ImplementationWithArg) {
                implementation.addParent(parent.implementation, parent.argument)
                parent.argument?.let { useTypes(it) }
            }
        }

        val parents = ParentsHolder()

        fun optInToInternals() {
            implementation.requiresOptIn = true
        }

        fun publicImplementation() {
            implementation.isPublic = true
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

        fun defaultBuiltInType(type: String) {
            default("coneTypeOrNull") {
                value = "StandardClassIds.$type.constructClassLikeType()"
                isMutable = false
            }
            useTypes(standardClassIdsType, constructClassLikeTypeImport)
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

        fun noSource() {
            defaultNull("source", withGetter = true)
        }

        fun defaultEmptyList(field: String) {
            require(getField(field).origin is FieldList) {
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

        var kind: Implementation.Kind?
            get() = implementation.kind
            set(value) {
                implementation.kind = value
            }

        inner class DefaultValueContext(private val field: FieldWithDefault) {
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
