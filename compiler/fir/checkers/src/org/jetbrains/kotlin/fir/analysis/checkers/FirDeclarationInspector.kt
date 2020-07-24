/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Provides representations for FirElement's.
 */
interface FirDeclarationPresenter {
    open class RepresentationBuilder {
        var receiver = ""
        var name = ""

        open fun build() = "[$receiver] $name"
    }

    fun buildRepresentation(init: RepresentationBuilder.() -> Unit): String {
        return RepresentationBuilder().apply(init).build()
    }

    class FunctionRepresentationBuilder : RepresentationBuilder() {
        var representsOperator = false
        var typeArguments = ""
        var parameters = ""

        override fun build() = "<$typeArguments> [$receiver] ${if (representsOperator) "operator " else ""}$name ($parameters)"
    }

    fun buildFunctionRepresentation(init: FunctionRepresentationBuilder.() -> Unit): String {
        return FunctionRepresentationBuilder().apply(init).build()
    }

    fun represent(it: FirElement) = "NO_REPRESENTATION"

    fun represent(it: ClassId) = it.packageFqName.asString() + '/' + it.relativeClassName.asString()

    fun represent(it: CallableId) = if (it.className != null) {
        it.packageName.asString() + '/' + it.className + '.' + it.callableName
    } else {
        it.packageName.asString() + '/' + it.callableName
    }

    fun represent(it: FirTypeRef) = when (it) {
        is FirResolvedTypeRef -> it.type.toString()
        is FirErrorTypeRef -> "ERROR"
        else -> "?"
    }

    fun represent(it: FirTypeParameter) = it.name.asString() + " : " + it.bounds
        .map { represent(it) }
        .sorted()
        .joinToString()

    fun represent(it: FirValueParameter): String {
        val prefix = if (it.isVararg) "vararg " else ""
        return prefix + " " + represent(it.returnTypeRef)
    }

    fun represent(it: FirProperty) = buildRepresentation {
        it.receiverTypeRef?.let {
            receiver = represent(it)
        }
        name = represent(it.symbol.callableId)
    }

    fun represent(it: FirSimpleFunction) = buildFunctionRepresentation {
        typeArguments = it.typeParameters.joinToString { represent(it) }
        it.receiverTypeRef?.let {
            receiver = represent(it)
        }
        representsOperator = it.isOperator
        name = represent(it.symbol.callableId)
        parameters = it.valueParameters.joinToString { represent(it) }
    }

    fun represent(it: FirTypeAlias) = buildRepresentation {
        name = represent(it.symbol.classId)
    }

    fun represent(it: FirRegularClass) = buildRepresentation {
        name = represent(it.symbol.classId)
    }
}

private class FirDefaultDeclarationPresenter : FirDeclarationPresenter

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

/**
 * Collects FirDeclarations for further analysis.
 */
class FirDeclarationInspector(
    private val presenter: FirDeclarationPresenter = FirDefaultDeclarationPresenter()
) {
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
            is FirRegularClass -> presenter.represent(declaration)
            is FirTypeAlias -> presenter.represent(declaration)
            is FirProperty -> presenter.represent(declaration)
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
        val key = presenter.represent(declaration)
        var value = functionDeclarations[key]

        if (value == null) {
            value = mutableListOf()
            functionDeclarations[key] = value
        }

        value.add(declaration)
    }

    fun contains(declaration: FirDeclaration) = when (declaration) {
        is FirSimpleFunction -> presenter.represent(declaration) in functionDeclarations
        is FirRegularClass -> presenter.represent(declaration) in otherDeclarations
        is FirTypeAlias -> presenter.represent(declaration) in otherDeclarations
        is FirProperty -> presenter.represent(declaration) in otherDeclarations
        else -> false
    }
}