/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.compositeTransformResultType
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.pureAbstractElementType


fun Implementation.collectImports(): List<String> {
    return element.collectImportsInternal(
        listOf(
            element.fullQualifiedName,
        )
                + usedTypes.mapNotNull { it.fullQualifiedName } + parents.mapNotNull { it.fullQualifiedName }
                + listOfNotNull(
            pureAbstractElementType.fullQualifiedName?.takeIf { needPureAbstractElement },
        ),
        isImpl = true,
    )
}

fun Element.collectImports(): List<String> {
    val baseTypes = parents.mapTo(mutableListOf()) { it.fullQualifiedName }
    baseTypes += parentsArguments.values.flatMap { it.values }.mapNotNull { it.fullQualifiedName }
    val isBaseFirElement = this == AbstractFirTreeBuilder.baseFirElement
    if (isBaseFirElement) {
        baseTypes += compositeTransformResultType.fullQualifiedName!!
    }
    if (needPureAbstractElement) {
        baseTypes += pureAbstractElementType.fullQualifiedName!!
    }
    return collectImportsInternal(
        baseTypes,
        isImpl = false,
    )
}

private fun Element.collectImportsInternal(base: List<String>, isImpl: Boolean): List<String> {
    val fqns = base + allFields.mapNotNull { it.fullQualifiedName } +
            allFields.flatMap { it.arguments.mapNotNull { it.fullQualifiedName } } +
            typeArguments.flatMap { it.upperBounds.mapNotNull { it.fullQualifiedName } }
    val realPackageName = if (isImpl) "$packageName.impl." else "$packageName."
    return fqns.filter { fqn ->
        fqn.dropLastWhile { it != '.' } != realPackageName
    }.distinct().sorted() + "$VISITOR_PACKAGE.*"
}

val KindOwner.needPureAbstractElement: Boolean
    get() = (kind != Implementation.Kind.Interface) && !allParents.any { it.kind == Implementation.Kind.AbstractClass }


val Field.isVal: Boolean get() = this is FieldList || (this is FieldWithDefault && origin is FieldList) || !isMutable


fun Field.transformFunctionDeclaration(returnType: String): String {
    return transformFunctionDeclaration(name.capitalize(), returnType)
}

fun transformFunctionDeclaration(transformName: String, returnType: String): String {
    return "fun <D> transform$transformName(transformer: FirTransformer<D>, data: D): $returnType"
}

fun Field.replaceFunctionDeclaration(): String {
    val capName = name.capitalize()
    return "fun replace$capName(new$capName: $typeWithArgumentsWithoutNullablity)"
}

val Field.mutableType: String
    get() = when (this) {
        is FieldList -> if (isMutable) "Mutable$typeWithArguments" else typeWithArguments
        is FieldWithDefault -> if (isMutable) origin.mutableType else typeWithArguments
        else -> typeWithArguments
    }

fun Field.call(): String = if (nullable) "?." else "."

fun Element.multipleUpperBoundsList(): String {
    return typeArguments.filterIsInstance<TypeArgumentWithMultipleUpperBounds>().takeIf { it.isNotEmpty() }?.let { arguments ->
        val upperBoundsList = arguments.joinToString(", ", postfix = " ") { argument ->
            argument.upperBounds.joinToString(", ") { upperBound -> "${argument.name} : ${upperBound.typeWithArguments}" }
        }
        " where $upperBoundsList"
    } ?: " "
}

fun Implementation.Kind?.braces(): String = when (this) {
    Implementation.Kind.Interface -> ""
    Implementation.Kind.OpenClass, Implementation.Kind.AbstractClass -> "()"
    else -> throw IllegalStateException(this.toString())
}

val Element.safeDecapitalizedName: String get() = if (name == "Class") "klass" else name.decapitalize()

val Importable.typeWithArguments: String
    get() = when (this) {
        is AbstractElement -> type + generics
        is Implementation -> type + element.generics
        is FirField -> element.typeWithArguments + if (nullable) "?" else ""
        is Field -> type + generics + if (nullable) "?" else ""
        is Type -> type + generics
        is ImplementationWithArg -> type + generics
        else -> throw IllegalArgumentException()
    }

val Importable.typeWithArgumentsWithoutNullablity: String get() = typeWithArguments.dropLastWhile { it == '?' }

val ImplementationWithArg.generics: String
    get() = argument?.let { "<${it.type}>" } ?: ""

val AbstractElement.generics: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""

val Field.generics: String
    get() = arguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.typeWithArguments } }
        ?: ""

val Element.typeParameters: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "<", "> ")
        ?: ""

val Type.generics: String
    get() = arguments.takeIf { it.isNotEmpty() }?.joinToString(",", "<", ">") ?: ""