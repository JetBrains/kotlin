/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*

sealed class Field : AbstractField<Field>() {
    abstract var withReplace: Boolean

    abstract var withTransform: Boolean
    open var needTransformInOtherChildren: Boolean = false

    var withBindThis = true

    override var defaultValueInBuilder: String? = null

    override var customSetter: String? = null

    abstract override var isVolatile: Boolean

    abstract override var isFinal: Boolean

    abstract override var isParameter: Boolean

    abstract override var isMutable: Boolean

    val receiveNullableTypeInReplace: Boolean
        get() = typeRef.nullable || overriddenFields.any { it.typeRef.nullable }

    override fun updateFieldsInCopy(copy: Field) {
        super.updateFieldsInCopy(copy)
        copy.withTransform = withTransform
        copy.needTransformInOtherChildren = needTransformInOtherChildren
        copy.customInitializationCall = customInitializationCall
        copy.skippedInCopy = skippedInCopy
    }

    override fun updatePropertiesFromOverriddenFields(parentFields: List<Field>) {
        super.updatePropertiesFromOverriddenFields(parentFields)
        withTransform = withTransform || parentFields.any { it.withTransform }
        needTransformInOtherChildren = needTransformInOtherChildren || parentFields.any { it.needTransformInOtherChildren }
        withReplace = withReplace || parentFields.any { it.withReplace }
    }
}

class SingleField(
    override val name: String,
    override var typeRef: TypeRefWithNullability,
    override val isChild: Boolean,
    override var isMutable: Boolean,
    override var withReplace: Boolean,
    override var withTransform: Boolean,
    override var isVolatile: Boolean = false,
    override var isFinal: Boolean = false,
    override var isParameter: Boolean = false,
) : Field() {

    override fun internalCopy(): Field {
        return SingleField(
            name = name,
            typeRef = typeRef,
            isChild = isChild,
            isMutable = isMutable,
            withReplace = withReplace,
            withTransform = withTransform,
            isVolatile = isVolatile,
            isFinal = isFinal,
            isParameter = isParameter,
        ).apply {
            withBindThis = this@SingleField.withBindThis
        }
    }

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        typeRef = typeRef.substitute(map) as TypeRefWithNullability
    }
}
// ----------- Field list -----------

class FieldList(
    override val name: String,
    override var baseType: TypeRef,
    override var withReplace: Boolean,
    override var withTransform: Boolean,
    override val isChild: Boolean,
    val isMutableOrEmptyList: Boolean = false,
) : Field(), ListField {
    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = super.typeRef

    override val listType: ClassRef<PositionTypeParameterRef>
        get() = StandardTypes.list

    override var isVolatile: Boolean = false
    override var isFinal: Boolean = false
    override var isMutable: Boolean = true
    override var isParameter: Boolean = false

    override fun internalCopy(): Field {
        return FieldList(
            name,
            baseType,
            withReplace,
            withTransform,
            isChild,
            isMutableOrEmptyList
        )
    }

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        baseType = baseType.substitute(map)
    }
}
