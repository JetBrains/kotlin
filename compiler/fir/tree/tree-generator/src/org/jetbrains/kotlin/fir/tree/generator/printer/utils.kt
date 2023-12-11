/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.BASE_PACKAGE
import org.jetbrains.kotlin.fir.tree.generator.firTransformerType
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.FieldList
import org.jetbrains.kotlin.fir.tree.generator.model.FieldWithDefault
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.utils.SmartPrinter

context(ImportCollector)
fun SmartPrinter.transformFunctionDeclaration(
    field: Field,
    returnType: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    transformFunctionDeclaration(field.name.replaceFirstChar(Char::uppercaseChar), returnType, override, implementationKind)
}

context(ImportCollector)
fun SmartPrinter.transformOtherChildrenFunctionDeclaration(
    element: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    transformFunctionDeclaration("OtherChildren", element, override, implementationKind)
}

context(ImportCollector)
private fun SmartPrinter.transformFunctionDeclaration(
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

context(ImportCollector)
fun SmartPrinter.replaceFunctionDeclaration(
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
    )
}

fun Field.getMutableType(forBuilder: Boolean = false): TypeRefWithNullability = when (this) {
    is FieldList -> when {
        isMutableOrEmptyList && !forBuilder -> type(BASE_PACKAGE, "MutableOrEmptyList", kind = TypeKind.Class)
        isMutable -> StandardTypes.mutableList
        else -> StandardTypes.list
    }.withArgs(baseType).copy(nullable)
    is FieldWithDefault -> if (isMutable) origin.getMutableType() else typeRef
    else -> typeRef
}
