/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

import org.jetbrains.kotlin.types.model.*

abstract class BaseKotlinMangleComputer<
        Declaration : Any,
        Type : KotlinTypeMarker,
        TypeParameter : TypeParameterMarker,
        ValueParameter : Declaration,
        TypeParameterContainer : Declaration,
        FunctionDeclaration : Declaration,
        >(protected val builder: StringBuilder, protected val mode: MangleMode) : KotlinMangleComputer<Declaration> {

    protected abstract val typeSystemContext: TypeSystemContext

    protected val typeParameterContainers = ArrayList<TypeParameterContainer>(4)

    protected var isRealExpect = false

    protected open fun FunctionDeclaration.platformSpecificFunctionName(): String? = null

    protected open fun FunctionDeclaration.platformSpecificSuffix(): String? = null

    protected open fun FunctionDeclaration.specialValueParamPrefix(param: ValueParameter): String = ""

    protected open fun addReturnType(): Boolean = false

    protected open fun addReturnTypeSpecialCase(function: FunctionDeclaration): Boolean = false

    protected fun StringBuilder.appendName(s: String) {
        if (mode.fqn) {
            append(s)
        }
    }

    protected fun StringBuilder.appendName(c: Char) {
        if (mode.fqn) {
            append(c)
        }
    }

    protected fun StringBuilder.appendSignature(s: String) {
        if (mode.signature) {
            append(s)
        }
    }

    protected fun StringBuilder.appendSignature(c: Char) {
        if (mode.signature) {
            append(c)
        }
    }

    protected fun StringBuilder.appendSignature(i: Int) {
        if (mode.signature) {
            append(i)
        }
    }

    protected abstract fun Declaration.visitParent()

    protected abstract fun Declaration.visit()

    final override fun computeMangle(declaration: Declaration): String {
        declaration.visit()
        return builder.toString()
    }

    protected fun Declaration.mangleSimpleDeclaration(name: String) {
        val l = builder.length
        visitParent()

        if (builder.length != l) {
            builder.appendName(MangleConstant.FQN_SEPARATOR)
        }

        builder.appendName(name)
    }

    protected abstract fun mangleType(tBuilder: StringBuilder, type: Type)

    protected open fun mangleTypePlatformSpecific(type: Type, tBuilder: StringBuilder) {}

    protected abstract fun isVararg(valueParameter: ValueParameter): Boolean

    protected abstract fun getValueParameterType(valueParameter: ValueParameter): Type

    protected abstract fun getIndexOfTypeParameter(typeParameter: TypeParameter, container: TypeParameterContainer): Int

    protected fun StringBuilder.mangleTypeParameterReference(typeParameter: TypeParameter) {
        val parent = getEffectiveParent(typeParameter)
        val ci = typeParameterContainers.indexOf(parent)
        // TODO: what should we do in this case?
//            require(ci >= 0) { "No type container found for ${typeParameter.render()}" }
        appendSignature(ci)
        appendSignature(MangleConstant.INDEX_SEPARATOR)
        appendSignature(getIndexOfTypeParameter(typeParameter, parent))
    }


    protected fun mangleTypeParameter(tpBuilder: StringBuilder, param: TypeParameter, index: Int) = with(typeSystemContext) {
        tpBuilder.appendSignature(index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.getUpperBounds().collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) {
            @Suppress("UNCHECKED_CAST")
            mangleType(this, it as Type)
        }
    }

    protected abstract fun getEffectiveParent(typeParameter: TypeParameter): TypeParameterContainer

    protected fun mangleValueParameter(vpBuilder: StringBuilder, param: ValueParameter) {
        mangleType(vpBuilder, getValueParameterType(param))

        if (isVararg(param)) vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
    }

    protected fun mangleTypeArguments(tBuilder: StringBuilder, type: Type) = with(typeSystemContext) {
        val typeArguments = type.getArguments().zip(type.typeConstructor().getParameters())
        if (typeArguments.isEmpty()) return
        @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY")
        typeArguments.collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { (typeArgument, typeParameter) ->
            when {
                typeArgument.isStarProjection() -> appendSignature(MangleConstant.STAR_MARK)
                else -> {
                    // FIXME: Use effective variance here according to the klib spec: `org.jetbrains.kotlin.types.AbstractTypeChecker.effectiveVariance(typeParameter.getVariance(), typeArgument.getVariance())`
                    // NOTE: If we start using effective variance instead of declared variance, we must take into account
                    // binary compatibility implications.
                    val variance = typeArgument.getVariance()
                    if (variance != TypeVariance.INV) {
                        appendSignature(variance.presentation)
                        appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                    }

                    @Suppress("UNCHECKED_CAST")
                    mangleType(this, typeArgument.getType() as Type)
                }
            }
        }
    }
}
