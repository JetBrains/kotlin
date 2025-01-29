/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.generators.tree.ListField as AbstractListField

sealed class Field(
    override val name: String,
    override var isMutable: Boolean,
) : AbstractField<Field>() {
    abstract val symbolClass: Symbol?

    override var defaultValueInBuilder: String?
        get() = null
        set(_) = error("Builders are not supported")

    override var customSetter: String? = null

    override fun toString() = "$name: $typeRef"

    override var isFinal: Boolean = false

    /**
     * Indicates whether the field is always excluded from constructor arguments when generating `DeepCopyIrTreeWithSymbols`.
     */
    var deepCopyExcludeFromConstructor: Boolean = false

    /**
     * Indicates whether the field is always excluded from apply operations when generating `DeepCopyIrTreeWithSymbols`.
     */
    var deepCopyExcludeFromApply: Boolean = false

    /**
     * Specifies whether field with a symbol should not be processed by `SymbolRemapper`.
     */
    var deepCopyExcludeFromSymbolRemapper: Boolean = false

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.customSetter = customSetter
        copy.symbolFieldRole = symbolFieldRole
        copy.deepCopyExcludeFromConstructor = deepCopyExcludeFromConstructor
        copy.deepCopyExcludeFromApply = deepCopyExcludeFromApply
        copy.deepCopyExcludeFromSymbolRemapper = deepCopyExcludeFromSymbolRemapper
    }
}

class SimpleField(
    name: String,
    override var typeRef: TypeRefWithNullability,
    mutable: Boolean,
    override val isChild: Boolean,
) : Field(name, mutable) {

    override val symbolClass: Symbol?
        get() = (typeRef as? ElementOrRef<*>)?.element as? Symbol

    override val containsElement: Boolean
        get() = (typeRef as? ElementOrRef<*>)?.element is Element

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        typeRef = typeRef.substitute(map) as TypeRefWithNullability
    }

    override fun internalCopy() = SimpleField(name, typeRef, isMutable, isChild)
}

class ListField(
    name: String,
    override var baseType: TypeRef,
    private val isNullable: Boolean,
    val mutability: Mutability,
    override val isChild: Boolean,
) : Field(name, mutability == Mutability.Var), AbstractListField {

    override val listType: ClassRef<PositionTypeParameterRef> = when (mutability) {
        Mutability.MutableList -> StandardTypes.mutableList
        Mutability.Array -> StandardTypes.array
        Mutability.Var -> StandardTypes.list
    }

    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = listType.withArgs(baseType).copy(isNullable)

    override val symbolClass: Symbol?
        get() = (baseType as? ElementOrRef<*>)?.element as? Symbol

    override val containsElement: Boolean
        get() = (baseType as? ElementOrRef<*>)?.element is Element

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        baseType = baseType.substitute(map)
    }

    override fun internalCopy() =
        ListField(name, baseType, isNullable, mutability, isChild)

    enum class Mutability {
        Var,
        MutableList,
        Array
    }
}
