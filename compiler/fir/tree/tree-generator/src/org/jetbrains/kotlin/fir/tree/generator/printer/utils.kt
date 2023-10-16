/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.firTransformerType
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.*

val Field.isVal: Boolean
    get() = (this is FieldList && !isMutableOrEmpty) || (this is FieldWithDefault && origin is FieldList && !origin.isMutableOrEmpty) || !isMutable

context(ImportCollector)
fun Field.transformFunctionDeclaration(returnType: TypeRef): String {
    return transformFunctionDeclaration(name.replaceFirstChar(Char::uppercaseChar), returnType)
}

context(ImportCollector)
fun transformFunctionDeclaration(transformName: String, returnType: TypeRef): String {
    return "fun <D> transform$transformName(transformer: ${firTransformerType.render()}<D>, data: D): " +
            returnType.render()
}

context(ImportCollector)
fun Field.replaceFunctionDeclaration(
    overridenType: TypeRefWithNullability? = null,
    forceNullable: Boolean = false,
): String {
    val capName = name.replaceFirstChar(Char::uppercaseChar)
    val type = overridenType ?: typeRef
    val typeWithNullable = if (forceNullable) type.copy(nullable = true) else type
    return "fun replace$capName(new$capName: ${typeWithNullable.render()})"
}

fun Field.getMutableType(forBuilder: Boolean = false): TypeRef = when (this) {
    is FieldList -> when {
        isMutableOrEmpty && !forBuilder -> type(BASE_PACKAGE, "MutableOrEmptyList", kind = TypeKind.Class)
        isMutable -> StandardTypes.mutableList
        else -> StandardTypes.list
    }.withArgs(baseType).copy(nullable)
    is FieldWithDefault -> if (isMutable) origin.getMutableType() else typeRef
    else -> typeRef
}

fun Field.call(): String = if (nullable) "?." else "."

val Element.safeDecapitalizedName: String get() = if (name == "Class") "klass" else name.replaceFirstChar(Char::lowercaseChar)
