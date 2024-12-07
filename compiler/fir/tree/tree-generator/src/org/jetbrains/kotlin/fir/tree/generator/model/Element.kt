/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.fir.tree.generator.BASE_PACKAGE
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(name: String, override val propertyName: String, kind: Kind) : AbstractElement<Element, Field, Implementation>(name) {
    companion object {
        private val allowedKinds = setOf(
            ImplementationKind.Interface,
            ImplementationKind.SealedInterface,
            ImplementationKind.AbstractClass,
            ImplementationKind.SealedClass
        )
    }

    override val namePrefix: String
        get() = "Fir"

    override val packageName: String = BASE_PACKAGE + kind.packageName.let { if (it.isBlank()) it else "." + it }

    override var kind: ImplementationKind?
        get() = super.kind
        set(value) {
            if (value !in allowedKinds) {
                throw IllegalArgumentException(value.toString())
            }
            super.kind = value
        }
    var _needTransformOtherChildren: Boolean = false

    override val hasAcceptMethod: Boolean
        get() = true

    override val hasTransformMethod: Boolean
        get() = true

    override val hasAcceptChildrenMethod: Boolean
        get() = isRootElement

    override val hasTransformChildrenMethod: Boolean
        get() = isRootElement

    override val walkableChildren: List<Field>
        get() = emptyList() // Use Implementation#walkableChildren instead

    override val transformableChildren: List<Field>
        get() = emptyList() // Use Implementation#transformableChildren instead

    override val visitorParameterName: String
        get() = safeDecapitalizedName

    val needTransformOtherChildren: Boolean get() = _needTransformOtherChildren || elementParents.any { it.element.needTransformOtherChildren }

    operator fun FieldSet.unaryPlus() {
        val fields = fieldDefinitions.map { it.copy() }
        this@Element.fields.addAll(fields)
    }

    enum class Kind(val packageName: String) {
        Expression("expressions"),
        Declaration("declarations"),
        Reference("references"),
        TypeRef("types"),
        Contracts("contracts"),
        Diagnostics("diagnostics"),
        Other("")
    }
}

typealias ElementRef = GenericElementRef<Element>

typealias ElementOrRef = GenericElementOrRef<Element>
