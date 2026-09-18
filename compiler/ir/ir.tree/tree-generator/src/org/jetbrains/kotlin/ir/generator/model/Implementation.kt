/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.AbstractField.ImplementationDefaultStrategy
import org.jetbrains.kotlin.generators.tree.AbstractImplementation
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter

class Implementation(element: Element, name: String?) : AbstractImplementation<Implementation, Element, Field>(element, name) {
    override val allFields: List<Field> = element.allFields.map { it.copy() }

    override var kind: ImplementationKind? = ImplementationKind.FinalClass

    var generationCallback: (ImportCollectingPrinter.() -> Unit)? = null

    var hasConstructorIndicator = false
    var bindOwnedSymbol = true
    override var doPrint = true

    init {
        isPublic = true
    }

    /**
     * The fields that a generated builder initializes on the element *after* it has been constructed.
     *
     * Unlike FIR, an `Ir*Impl` accepts only a part of its fields in its constructor and declares the rest as stored properties with
     * a default value. `IrFactory` already bridges that gap by hand -- see `IrFactory.createClass`, which constructs an
     * `IrClassImpl` and then assigns `isCompanion`, `isInner`, `isData`, ... on the result. Generated builders do the same, so that
     * from a caller's point of view an element has one flat set of properties rather than two.
     *
     * These are copies of the implementation's fields, because a builder models some of them slightly differently, see
     * [Field.assignedInBuilderIfNotNull].
     */
    val fieldsAssignedInBuilder: List<Field> by lazy {
        fieldsInBody.mapNotNull { it.toBuilderFieldOrNull() }
    }

    override val fieldsForBuilder: List<Field> by lazy {
        fieldsInConstructor + fieldsAssignedInBuilder
    }
}

/**
 * Fields that no builder may initialize, whichever element they own.
 *
 * `attributeOwnerId` defaults to the element being constructed, which cannot be expressed before that element exists.
 */
private val fieldsNeverInBuilder = setOf("attributeOwnerId")

/**
 * Returns how a builder should expose this body field, or `null` if a builder cannot meaningfully initialize it.
 *
 * A *list of elements* is left out -- `declarations`, `typeParameters`, `annotations` -- because copying one onto the built
 * element raises the question of who sets `parent` on its contents, which no builder answers. Callers keep populating those on
 * the built element, as they do now. A list of anything else, such as `IrTypeParameter.superTypes`, is plain data and is
 * exposed like any other field, as is a single child slot such as `IrValueParameter.defaultValue`.
 */
private fun Field.toBuilderFieldOrNull(): Field? {
    if (name in fieldsNeverInBuilder || this is MapField) return null
    if (this is ListField) {
        if (containsElement) return null
        // A list has no "unset" state to fall back on, so it is always exposed and always copied onto the element.
        return copy()
    }
    if (this !is SimpleField) return null
    return when (val defaultStrategy = implementationDefaultStrategy) {
        // A computed property has no backing field to assign. This also covers the fields whose setter deliberately throws, such
        // as `IrFile.startOffset`, which are configured with `defaultWithErrorOnSet`.
        is ImplementationDefaultStrategy.DefaultValue -> when {
            defaultStrategy.withGetter || !isMutable -> null
            else -> copy().also { it.defaultValueInBuilder = defaultStrategy.defaultValue }
        }
        // A `lateinit` field has no value to fall back on, so the builder exposes it as nullable and assigns it only if it was
        // set, leaving the element's property uninitialized otherwise, exactly as `IrFactory` does.
        ImplementationDefaultStrategy.Lateinit -> (copy() as SimpleField).also {
            it.typeRef = it.typeRef.copy(nullable = true)
            it.isMutable = true
            it.defaultValueInBuilder = "null"
            it.assignedInBuilderIfNotNull = true
        }
        // `Required` fields are constructor parameters, and so are not part of `fieldsInBody` in the first place.
        else -> null
    }
}
