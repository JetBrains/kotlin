/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.BASE_PACKAGE
import org.jetbrains.kotlin.fir.tree.generator.firTransformerType
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.ListField
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration

fun ImportCollectingPrinter.transformFunctionDeclaration(
    field: Field,
    returnType: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    transformFunctionDeclaration(field.name.replaceFirstChar(Char::uppercaseChar), returnType, override, implementationKind)
}

fun ImportCollectingPrinter.transformOtherChildrenFunctionDeclaration(
    element: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    transformFunctionDeclaration("OtherChildren", element, override, implementationKind)
}

private fun ImportCollectingPrinter.transformFunctionDeclaration(
    transformName: String,
    returnType: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    val dataTP = TypeVariable("D")
    printFunctionDeclaration(
        name = "transform$transformName",
        parameters = listOf(
            FunctionParameter("transformer", firTransformerType.withArgs(dataTP)),
            FunctionParameter("data", dataTP),
        ),
        returnType = returnType,
        typeParameters = listOf(dataTP),
        modality = Modality.ABSTRACT.takeIf {
            implementationKind == ImplementationKind.AbstractClass || implementationKind == ImplementationKind.SealedClass
        },
        override = override,
    )
}

fun ImportCollectingPrinter.replaceFunctionDeclaration(
    field: Field,
    override: Boolean,
    implementationKind: ImplementationKind,
    overriddenType: TypeRefWithNullability? = null,
    forceNullable: Boolean = false,
) {
    val capName = field.name.replaceFirstChar(Char::uppercaseChar)
    val type = overriddenType ?: field.typeRef
    val typeWithNullable = if (forceNullable) type.copy(nullable = true) else type

    printFunctionDeclaration(
        name = "replace$capName",
        parameters = listOf(FunctionParameter("new$capName", typeWithNullable)),
        returnType = StandardTypes.unit,
        modality = Modality.ABSTRACT.takeIf {
            implementationKind == ImplementationKind.AbstractClass || implementationKind == ImplementationKind.SealedClass
        },
        override = override,
        optInAnnotation = field.replaceOptInAnnotation,
    )
}

fun Field.getMutableType(forBuilder: Boolean = false): TypeRefWithNullability = when (this) {
    is ListField -> when {
        forBuilder -> StandardTypes.mutableList
        !isMutable -> StandardTypes.list
        isMutableOrEmptyList -> type(BASE_PACKAGE, "MutableOrEmptyList", kind = TypeKind.Class)
        else -> StandardTypes.mutableList
    }.withArgs(baseType).copy(nullable)
    else -> typeRef
}
