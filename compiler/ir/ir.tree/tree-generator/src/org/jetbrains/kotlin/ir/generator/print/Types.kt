/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.config.ElementConfigOrRef
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Element.Companion.elementName2typeName
import org.jetbrains.kotlin.ir.generator.model.ElementRef
import org.jetbrains.kotlin.ir.generator.util.*
import org.jetbrains.kotlin.types.Variance

fun Element.toPoet() = ClassName(packageName, typeName)
fun Element.toPoetSelfParameterized() = toPoet().parameterizedByIfAny(poetTypeVariables)
fun Element.toPoetStarParameterized() = toPoet().parameterizedByIfAny(List(params.size) { STAR })
val Element.poetTypeVariables get() = params.map { TypeVariableName(it.name) }

fun TypeRef.toPoet(): TypeName {
    return when (this) {
        is ElementRef -> ClassName(element.packageName, element.typeName).parameterizedByIfAny(typeArgsToPoet())
        is ClassRef<*> -> ClassName(packageName, simpleNames).parameterizedByIfAny(typeArgsToPoet())
        is NamedTypeParameterRef -> TypeVariableName(name)
        is ElementConfigOrRef -> ClassName(this.element.category.packageName, elementName2typeName(element.name)).parameterizedByIfAny(
            typeArgsToPoet()
        ) // ad-hoc solution
        is TypeRef.Star -> STAR
        else -> error("Unexpected type reference: $this")
    }.let {
        if (this is TypeRefWithNullability) it.copy(nullable = nullable) else it
    }
}

private fun ParametrizedTypeRef<*, *>.typeArgsToPoet(): List<TypeName> {
    if (args.isEmpty()) {
        return emptyList()
    }

    fun fromPositional(args: Map<PositionTypeParameterRef, TypeRef>): List<TypeName> {
        val num = args.keys.maxOfOrNull { it.index }!!
        return (0..num).map { i -> args[PositionTypeParameterRef(i)]?.toPoet() ?: STAR }
    }

    val positional = args.keys.filterIsInstance<PositionTypeParameterRef>()
    if (positional.size == args.size) {
        @Suppress("UNCHECKED_CAST")
        return fromPositional(args as Map<PositionTypeParameterRef, TypeRef>)
    } else {
        check(positional.isEmpty()) { "Can't yet handle mixed index-name args" }
        this as ElementRef // Named args must only be used with generated elements (for now)

        val args = args.entries
            .associate { p -> PositionTypeParameterRef(element.params.withIndex().single { it.value.name == p.key.name }.index) to p.value }
        return fromPositional(args)
    }
}

fun TypeVariable.toPoet() = TypeVariableName(
    name, bounds.map { it.toPoet() }, when (variance) {
        Variance.INVARIANT -> null
        Variance.IN_VARIANCE -> KModifier.IN
        Variance.OUT_VARIANCE -> KModifier.OUT
    }
)

val ClassOrElementRef.typeKind: TypeKind
    get() = when (this) {
        is ElementRef -> element.kind!!.typeKind
        is ClassRef<*> -> kind
        else -> error("Unexpected type: $this")
    }
