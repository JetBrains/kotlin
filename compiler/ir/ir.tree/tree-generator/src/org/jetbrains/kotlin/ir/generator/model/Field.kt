/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ListField as AbstractListField

sealed class Field(
    override val name: String,
    override var isMutable: Boolean,
) : AbstractField<Field>(), AbstractFieldWithDefaultValue<Field> {
    var baseGetter: String? = null

    sealed class UseFieldAsParameterInIrFactoryStrategy {

        data object No : UseFieldAsParameterInIrFactoryStrategy()

        data class Yes(val defaultValue: String?) : UseFieldAsParameterInIrFactoryStrategy()
    }

    var customUseInIrFactoryStrategy: UseFieldAsParameterInIrFactoryStrategy? = null

    val useInIrFactoryStrategy: UseFieldAsParameterInIrFactoryStrategy
        get() = customUseInIrFactoryStrategy
            ?: if (isChild && containsElement) {
                UseFieldAsParameterInIrFactoryStrategy.No
            } else {
                UseFieldAsParameterInIrFactoryStrategy.Yes(null)
            }

    override var withGetter: Boolean
        get() = baseGetter != null
        set(value) = error("Operation not supported")

    override var defaultValueInImplementation: String? by ::baseGetter

    override var defaultValueInBuilder: String?
        get() = null
        set(_) = error("Builders are not supported")

    override val origin: Field
        get() = this

    override var customSetter: String? = null

    override fun toString() = "$name: $typeRef"

    override val isVolatile: Boolean
        get() = false

    override val isFinal: Boolean
        get() = baseGetter != null

    override val isParameter: Boolean
        get() = false

    override fun copy() = internalCopy().also(::updateFieldsInCopy)

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.baseGetter = baseGetter
        copy.customUseInIrFactoryStrategy = customUseInIrFactoryStrategy
        copy.customSetter = customSetter
    }

    protected abstract fun internalCopy(): Field
}

class SingleField(
    name: String,
    override var typeRef: TypeRefWithNullability,
    mutable: Boolean,
) : Field(name, mutable) {

    override fun replaceType(newType: TypeRefWithNullability) =
        SingleField(name, newType, isMutable).also(::updateFieldsInCopy)

    override fun internalCopy() = SingleField(name, typeRef, isMutable)
}

class ListField(
    name: String,
    override var baseType: TypeRef,
    private val isNullable: Boolean,
    override val listType: ClassRef<PositionTypeParameterRef>,
    mutable: Boolean,
) : Field(name, mutable), AbstractListField {

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType).copy(isNullable)

    override fun replaceType(newType: TypeRefWithNullability) = copy()

    override fun internalCopy() = ListField(name, baseType, isNullable, listType, isMutable)

    enum class Mutability {
        Var,
        MutableList,
        Array
    }
}
