/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.Importable

sealed class Field : AbstractField<Field>() {
    open var withReplace: Boolean = false

    open var needsSeparateTransform: Boolean = false
    open var needTransformInOtherChildren: Boolean = false

    open val isMutableOrEmptyList: Boolean
        get() = false

    var withBindThis = true

    override val origin: Field
        get() = this

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
        if (copy !is FieldWithDefault) {
            copy.needsSeparateTransform = needsSeparateTransform
            copy.needTransformInOtherChildren = needTransformInOtherChildren
            copy.customInitializationCall = customInitializationCall
            copy.skippedInCopy = skippedInCopy
        }
    }

    override fun updatePropertiesFromOverriddenFields(parentFields: List<Field>) {
        super.updatePropertiesFromOverriddenFields(parentFields)
        needsSeparateTransform = needsSeparateTransform || parentFields.any { it.needsSeparateTransform }
        needTransformInOtherChildren = needTransformInOtherChildren || parentFields.any { it.needTransformInOtherChildren }
        withReplace = withReplace || parentFields.any { it.withReplace }
    }
}

// ----------- Field with default -----------

class FieldWithDefault(override val origin: Field) : Field() {
    override val name: String get() = origin.name
    override val typeRef: TypeRefWithNullability get() = origin.typeRef
    override var isVolatile: Boolean = origin.isVolatile
    override var withReplace: Boolean
        get() = origin.withReplace
        set(_) {}
    override val isChild: Boolean
        get() = origin.isChild
    override val containsElement: Boolean
        get() = origin.containsElement
    override var needsSeparateTransform: Boolean
        get() = origin.needsSeparateTransform
        set(_) {}

    override var needTransformInOtherChildren: Boolean
        get() = origin.needTransformInOtherChildren
        set(_) {}

    override var isFinal: Boolean
        get() = origin.isFinal
        set(_) {}

    override var isParameter: Boolean
        get() = origin.isParameter
        set(_) {}

    override var customInitializationCall: String?
        get() = origin.customInitializationCall
        set(_) {}

    override var optInAnnotation: ClassRef<*>?
        get() = origin.optInAnnotation
        set(_) {}

    override var replaceOptInAnnotation: ClassRef<*>?
        get() = origin.replaceOptInAnnotation
        set(_) {}

    override var implementationDefaultStrategy: ImplementationDefaultStrategy? = origin.implementationDefaultStrategy
    override var defaultValueInBuilder: String? = null
    override var isMutable: Boolean = origin.isMutable
    override val isMutableOrEmptyList: Boolean
        get() = origin.isMutableOrEmptyList

    override var customSetter: String? = null
    override val overriddenFields: MutableSet<Field>
        get() = origin.overriddenFields

    override val arbitraryImportables: MutableList<Importable>
        get() = origin.arbitraryImportables

    override var skippedInCopy: Boolean
        get() = origin.skippedInCopy
        set(_) {}

    override fun internalCopy(): Field {
        return FieldWithDefault(origin).also {
            it.isMutable = isMutable
        }
    }

    override fun substituteType(map: TypeParameterSubstitutionMap) {}
}

class SimpleField(
    override val name: String,
    override var typeRef: TypeRefWithNullability,
    override val isChild: Boolean,
    override var isMutable: Boolean,
    override var withReplace: Boolean,
    override var isVolatile: Boolean = false,
    override var isFinal: Boolean = false,
    override var isParameter: Boolean = false,
) : Field() {

    override fun internalCopy(): Field {
        return SimpleField(
            name = name,
            typeRef = typeRef,
            isChild = isChild,
            isMutable = isMutable,
            withReplace = withReplace,
            isVolatile = isVolatile,
            isFinal = isFinal,
            isParameter = isParameter,
        ).apply {
            withBindThis = this@SimpleField.withBindThis
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
    override val isChild: Boolean,
    useMutableOrEmpty: Boolean = false,
) : Field(), ListField {
    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = super.typeRef

    override val listType: ClassRef<PositionTypeParameterRef>
        get() = StandardTypes.list

    override var isVolatile: Boolean = false
    override var isFinal: Boolean = false
    override var isMutable: Boolean = true
    override val isMutableOrEmptyList: Boolean = useMutableOrEmpty
    override var isParameter: Boolean = false

    override fun internalCopy(): Field {
        return FieldList(
            name,
            baseType,
            withReplace,
            isChild,
            isMutableOrEmptyList
        )
    }

    override fun substituteType(map: TypeParameterSubstitutionMap) {
        baseType = baseType.substitute(map)
    }
}
