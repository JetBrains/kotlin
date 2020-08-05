/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

private open class RepresentationBuilder {
    var receiver = ""
    var name = ""

    open fun build() = "[$receiver] $name"
}

private fun buildRepresentation(init: RepresentationBuilder.() -> Unit): String {
    return RepresentationBuilder().apply(init).build()
}

private class FunctionRepresentationBuilder : RepresentationBuilder() {
    var typeArguments = ""
    var parameters = ""

    override fun build() = "<$typeArguments> [$receiver] $name ($parameters)"
}

private fun buildFunctionRepresentation(init: FunctionRepresentationBuilder.() -> Unit): String {
    return FunctionRepresentationBuilder().apply(init).build()
}

private fun ClassId.represent() = packageFqName.asString() + '/' + relativeClassName.asString()

private fun CallableId.represent() = if (className != null) {
    packageName.asString() + '/' + className + '.' + callableName
} else {
    packageName.asString() + '/' + callableName
}

private fun FirTypeRef.represent() = when (this) {
    is FirResolvedTypeRef -> type.toString()
    is FirErrorTypeRef -> "ERROR"
    else -> "?"
}

private fun FirTypeParameter.represent() = name.asString() + " : " + bounds
    .map { it.represent() }
    .sorted()
    .joinToString()

private fun FirValueParameter.represent(): String {
    val prefix = if (this.isVararg) "vararg " else ""
    return prefix + " " + this.returnTypeRef.represent()
}

private fun FirProperty.represent() = buildRepresentation {
    receiver = receiverTypeRef?.represent() ?: ""
    name = symbol.callableId.represent()
}

private fun FirSimpleFunction.represent() = buildFunctionRepresentation {
    typeArguments = typeParameters.joinToString { it.represent() }
    receiver = receiverTypeRef?.represent() ?: ""
    name = symbol.callableId.represent()
    parameters = valueParameters.joinToString { it.represent() }
}

private fun FirTypeAlias.represent() = buildRepresentation {
    name = symbol.classId.represent()
}

private fun FirRegularClass.represent() = buildRepresentation {
    name = symbol.classId.represent()
}

private val NO_NAME_PROVIDED = Name.special("<no name provided>")

// - see testEnumValuesValueOf.
// it generates a static function that has
// the same signature as the function defined
// explicitly.
// - see tests with `fun () {}`.
// you can't redeclare something that has no name.
private fun FirDeclaration.isCollectable() = when (this) {
    is FirSimpleFunction -> source?.kind !is FirFakeSourceElementKind && name != NO_NAME_PROVIDED
    is FirRegularClass -> name != NO_NAME_PROVIDED
    else -> true
}

class FirDeclarationInspector {
    val otherDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()
    val functionDeclarations = mutableMapOf<String, MutableList<FirSimpleFunction>>()

    fun collect(declaration: FirDeclaration) {
        if (!declaration.isCollectable()) {
            return
        }

        if (declaration is FirSimpleFunction) {
            return collectFunction(declaration)
        }

        val key = when (declaration) {
            is FirRegularClass -> declaration.represent()
            is FirTypeAlias -> declaration.represent()
            is FirProperty -> declaration.represent()
            else -> return
        }

        var value = otherDeclarations[key]

        if (value == null) {
            value = mutableListOf()
            otherDeclarations[key] = value
        }

        value.add(declaration)
    }

    private fun collectFunction(declaration: FirSimpleFunction) {
        val key = declaration.represent()
        var value = functionDeclarations[key]

        if (value == null) {
            value = mutableListOf()
            functionDeclarations[key] = value
        }

        value.add(declaration)
    }

    private fun contains(declaration: FirDeclaration) = when (declaration) {
        is FirSimpleFunction -> declaration.represent() in functionDeclarations
        is FirRegularClass -> declaration.represent() in otherDeclarations
        is FirTypeAlias -> declaration.represent() in otherDeclarations
        is FirProperty -> declaration.represent() in otherDeclarations
        else -> false
    }
}