/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef

sealed class Field : AbstractField() {
    open var withReplace: Boolean = false
    abstract val isFirType: Boolean

    open var needsSeparateTransform: Boolean = false
    var parentHasSeparateTransform: Boolean = true
    open var needTransformInOtherChildren: Boolean = false

    /**
     * @see org.jetbrains.kotlin.fir.tree.generator.util.detectBaseTransformerTypes
     */
    var useInBaseTransformerDetection = true

    open var customInitializationCall: String? = null

    open val isMutableOrEmptyList: Boolean
        get() = false

    open var isMutableInInterface: Boolean = false
    open val fromDelegate: Boolean get() = false

    open val overridenTypes: MutableSet<TypeRefWithNullability> = mutableSetOf()
    open var useNullableForReplace: Boolean = false
    open var notNull: Boolean = false

    var withBindThis = true

    abstract override var isVolatile: Boolean

    abstract override var isFinal: Boolean

    abstract override var isLateinit: Boolean

    abstract override var isParameter: Boolean

    abstract override var isMutable: Boolean

    fun copy(): Field = internalCopy().also {
        updateFieldsInCopy(it)
    }

    protected fun updateFieldsInCopy(copy: Field) {
        if (copy !is FieldWithDefault) {
            copy.needsSeparateTransform = needsSeparateTransform
            copy.needTransformInOtherChildren = needTransformInOtherChildren
            copy.useInBaseTransformerDetection = useInBaseTransformerDetection
            copy.isMutable = isMutable
            copy.overridenTypes += overridenTypes
            copy.arbitraryImportables += arbitraryImportables
            copy.useNullableForReplace = useNullableForReplace
            copy.customInitializationCall = customInitializationCall
            copy.optInAnnotation = optInAnnotation
        }
        copy.fromParent = fromParent
        copy.parentHasSeparateTransform = parentHasSeparateTransform
        copy.kDoc = kDoc
    }

    protected abstract fun internalCopy(): Field

}

// ----------- Field with default -----------

class FieldWithDefault(val origin: Field) : Field() {
    override val name: String get() = origin.name
    override val typeRef: TypeRefWithNullability get() = origin.typeRef
    override var isVolatile: Boolean = origin.isVolatile
    override var withReplace: Boolean
        get() = origin.withReplace
        set(_) {}
    override val isFirType: Boolean get() = origin.isFirType
    override var needsSeparateTransform: Boolean
        get() = origin.needsSeparateTransform
        set(_) {}

    override var needTransformInOtherChildren: Boolean
        get() = origin.needTransformInOtherChildren
        set(_) {}

    override var isFinal: Boolean
        get() = origin.isFinal
        set(_) {}

    override var isLateinit: Boolean
        get() = origin.isLateinit
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

    override var defaultValueInImplementation: String? = origin.defaultValueInImplementation
    var defaultValueInBuilder: String? = null
    override var isMutable: Boolean = origin.isMutable
    override val isMutableOrEmptyList: Boolean
        get() = origin.isMutableOrEmptyList

    override var isMutableInInterface: Boolean = origin.isMutableInInterface
    override var withGetter: Boolean = false
    override var customSetter: String? = null
    override var fromDelegate: Boolean = false
    var needAcceptAndTransform: Boolean = true
    override val overridenTypes: MutableSet<TypeRefWithNullability>
        get() = origin.overridenTypes

    override val arbitraryImportables: MutableList<Importable>
        get() = origin.arbitraryImportables

    override var useNullableForReplace: Boolean
        get() = origin.useNullableForReplace
        set(_) {}

    override fun internalCopy(): Field {
        return FieldWithDefault(origin).also {
            it.defaultValueInImplementation = defaultValueInImplementation
            it.isMutable = isMutable
            it.withGetter = withGetter
            it.fromDelegate = fromDelegate
            it.needAcceptAndTransform = needAcceptAndTransform
        }
    }
}

class SimpleField(
    override val name: String,
    override val typeRef: TypeRefWithNullability,
    override var withReplace: Boolean,
    override var isVolatile: Boolean = false,
    override var isFinal: Boolean = false,
    override var isLateinit: Boolean = false,
    override var isParameter: Boolean = false,
) : Field() {
    override val isFirType: Boolean = false
    override var isMutable: Boolean = withReplace

    override fun internalCopy(): Field {
        return SimpleField(
            name = name,
            typeRef = typeRef,
            withReplace = withReplace,
            isVolatile = isVolatile,
            isFinal = isFinal,
            isLateinit = isLateinit,
            isParameter = isParameter,
        ).apply {
            withBindThis = this@SimpleField.withBindThis
        }
    }

    fun replaceType(newType: TypeRefWithNullability) = SimpleField(
        name = name,
        typeRef = newType,
        withReplace = withReplace,
        isVolatile = isVolatile,
        isFinal = isFinal,
        isLateinit = isLateinit,
        isParameter = isParameter
    ).also {
        it.withBindThis = withBindThis
        updateFieldsInCopy(it)
    }
}

class FirField(
    override val name: String,
    val element: ElementRef,
    override var withReplace: Boolean,
) : Field() {

    override val typeRef: TypeRefWithNullability
        get() = element
    override var isVolatile: Boolean = false
    override var isFinal: Boolean = false
    override val isFirType: Boolean = true

    override var isMutable: Boolean = true
    override var isLateinit: Boolean = false
    override var isParameter: Boolean = false

    override fun internalCopy(): Field {
        return FirField(
            name,
            element,
            withReplace
        ).apply {
            withBindThis = this@FirField.withBindThis
        }
    }
}

// ----------- Field list -----------

class FieldList(
    override val name: String,
    val baseType: TypeRef,
    override var withReplace: Boolean,
    useMutableOrEmpty: Boolean = false
) : Field() {
    override var defaultValueInImplementation: String? = null
    override val typeRef: ClassRef<PositionTypeParameterRef>
        get() = StandardTypes.list.withArgs(baseType)

    override var isVolatile: Boolean = false
    override var isFinal: Boolean = false
    override var isMutable: Boolean = true
    override val isMutableOrEmptyList: Boolean = useMutableOrEmpty
    override var isLateinit: Boolean = false
    override var isParameter: Boolean = false

    override fun internalCopy(): Field {
        return FieldList(
            name,
            baseType,
            withReplace,
            isMutableOrEmptyList
        )
    }

    override val isFirType: Boolean = baseType is GenericElementOrRef<*, *> && baseType.element is Element
}
