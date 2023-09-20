/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.util.*

class Config(
    val elements: List<ElementConfig>,
    val rootElement: ElementConfig,
)

class ElementConfig(
    val propertyName: String,
    val name: String,
    val category: Category
) : ElementConfigOrRef {
    val params = mutableListOf<TypeVariable>()
    val parents = mutableListOf<TypeRef>()
    val fields = mutableListOf<FieldConfig>()
    val additionalImports = mutableListOf<Import>()

    var visitorName: String? = null
    var visitorParent: ElementConfig? = null
    var visitorParam: String? = null
    var accept = false // By default, accept is generated only for leaves.
    var transform = false
    var transformByChildren = false
    var transformerReturnType: ElementConfig? = null
    var childrenOrderOverride: List<String>? = null

    var ownsChildren = true // If false, acceptChildren/transformChildren will NOT be generated.

    var generateIrFactoryMethod = category == Category.Declaration
    val additionalIrFactoryMethodParameters = mutableListOf<FieldConfig>()
    val fieldsToSkipInIrFactoryMethod = hashSetOf<String>()

    /**
     * Set this to `true` if the element should be a leaf semantically, but technically it's not.
     *
     * For example, we only generate [IrFactory] methods for leaf elements. If we want to generate a method for this element, but it has
     * subclasses, it can be done by setting this property to `true`.
     */
    var isForcedLeaf = false

    var typeKind: TypeKind? = null

    var generationCallback: (TypeSpec.Builder.() -> Unit)? = null
    var kDoc: String? = null

    override val element get() = this
    override val args get() = emptyMap<NamedTypeParameterRef, TypeRef>()
    override val nullable get() = false
    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementConfigRef(this, args, false)
    override fun copy(nullable: Boolean) = ElementConfigRef(this, args, nullable)

    operator fun TypeVariable.unaryPlus() = apply {
        params.add(this)
    }

    operator fun FieldConfig.unaryPlus() = apply {
        fields.add(this)
    }

    override fun toString() = element.name

    override val packageName: String
        get() = category.packageName

    override val type: String
        get() = Element.elementName2typeName(name)

    override fun getTypeWithArguments(notNull: Boolean): String = type

    enum class Category(private val packageDir: String, val defaultVisitorParam: String) {
        Expression("expressions", "expression"),
        Declaration("declarations", "declaration"),
        Other("", "element");

        val packageName: String get() = BASE_PACKAGE + if (packageDir.isNotEmpty()) ".$packageDir" else ""
    }
}

sealed interface ElementConfigOrRef : ParametrizedTypeRef<ElementConfigOrRef, NamedTypeParameterRef>, TypeRefWithNullability {
    val element: ElementConfig
}

class ElementConfigRef(
    override val element: ElementConfig,
    override val args: Map<NamedTypeParameterRef, TypeRef>,
    override val nullable: Boolean
) : ElementConfigOrRef {
    override fun copy(args: Map<NamedTypeParameterRef, TypeRef>) = ElementConfigRef(element, args, nullable)
    override fun copy(nullable: Boolean) = ElementConfigRef(element, args, nullable)

    override fun toString() = element.name

    override val type: String
        get() = element.type

    override val packageName: String
        get() = element.packageName

    override fun getTypeWithArguments(notNull: Boolean): String = type + generics
}

sealed class UseFieldAsParameterInIrFactoryStrategy {

    data object No : UseFieldAsParameterInIrFactoryStrategy()

    data class Yes(val defaultValue: CodeBlock?) : UseFieldAsParameterInIrFactoryStrategy()
}

sealed class FieldConfig(
    val name: String,
    val isChild: Boolean,
) {
    var baseDefaultValue: CodeBlock? = null
    var baseGetter: CodeBlock? = null
    var printProperty = true
    var strictCastInTransformChildren = false

    internal var useFieldInIrFactoryStrategy: UseFieldAsParameterInIrFactoryStrategy =
        if (isChild) UseFieldAsParameterInIrFactoryStrategy.No else UseFieldAsParameterInIrFactoryStrategy.Yes(null)

    fun useFieldInIrFactory(defaultValue: CodeBlock? = null) {
        useFieldInIrFactoryStrategy = UseFieldAsParameterInIrFactoryStrategy.Yes(defaultValue)
    }

    fun useFieldInIrFactory(defaultValue: Boolean) {
        useFieldInIrFactoryStrategy = UseFieldAsParameterInIrFactoryStrategy.Yes(code("%L", defaultValue))
    }

    fun skipInIrFactory() {
        useFieldInIrFactoryStrategy = UseFieldAsParameterInIrFactoryStrategy.No
    }

    var kdoc: String? = null

    var generationCallback: (PropertySpec.Builder.() -> Unit)? = null

    override fun toString() = name
}

class SimpleFieldConfig(
    name: String,
    val type: TypeRef?,
    val nullable: Boolean,
    val mutable: Boolean,
    isChildElement: Boolean,
) : FieldConfig(name, isChildElement)

class ListFieldConfig(
    name: String,
    val elementType: TypeRef?,
    val nullable: Boolean,
    val mutability: Mutability,
    isChildElement: Boolean,
) : FieldConfig(name, isChildElement) {
    enum class Mutability {
        Immutable,
        Var,
        List,
        Array
    }
}
