/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

import org.jetbrains.kotlin.types.model.*

/**
 * A base implementation of the [KotlinMangleComputer] interface containing routines that are common for all frontends and backends.
 *
 * @param Declaration A class representing a Kotlin declaration.
 * @param Type A class representing a Kotlin type.
 * @param TypeParameter A class representing a type parameter of a Kotlin class or function.
 * @param ValueParameter A class representing a value parameter declaration of a Kotlin function.
 * @param TypeParameterContainer A class representing something that can have type parameters, like a Kotlin function or class declaration.
 * @param FunctionDeclaration A class representing a Kotlin function declaration.
 * @param Session An additional context used for type computations.
 * @property builder A string builder to write the mangled name into.
 * @property mode The mangle mode.
 * @property allowOutOfScopeTypeParameters Whether to throw an exception if the container of a referenced type parameter is not found
 * in the current scope. This often happens in the lowered IR and should never happen in the IR emitted by the frontend.
 */
abstract class BaseKotlinMangleComputer<Declaration, Type, TypeParameter, ValueParameter, TypeParameterContainer, FunctionDeclaration, Session>(
    protected val builder: StringBuilder,
    protected val mode: MangleMode,
    protected val allowOutOfScopeTypeParameters: Boolean = false,
) : KotlinMangleComputer<Declaration>
        where Declaration : Any,
              Type : KotlinTypeMarker,
              TypeParameter : TypeParameterMarker,
              ValueParameter : Declaration,
              TypeParameterContainer : Declaration,
              FunctionDeclaration : Declaration {

    /**
     * The type system to use to query properties of types, type parameters and type arguments.
     */
    protected abstract fun getTypeSystemContext(session: Session): TypeSystemContext

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

    protected abstract fun mangleType(tBuilder: StringBuilder, type: Type, declarationSiteSession: Session)

    protected open fun mangleTypePlatformSpecific(type: Type, tBuilder: StringBuilder) {}

    protected abstract fun isVararg(valueParameter: ValueParameter): Boolean

    protected abstract fun getValueParameterType(valueParameter: ValueParameter): Type

    protected abstract fun getIndexOfTypeParameter(typeParameter: TypeParameter, container: TypeParameterContainer): Int

    /**
     * Used to show a meaningful exception message.
     */
    protected abstract fun renderDeclaration(declaration: Declaration): String

    protected abstract fun getTypeParameterName(typeParameter: TypeParameter): String

    protected fun StringBuilder.mangleTypeParameterReference(typeParameter: TypeParameter) {
        val parent = getEffectiveParent(typeParameter)
        val containerIndex = typeParameterContainers.indexOf(parent)
        require(allowOutOfScopeTypeParameters || containerIndex >= 0) {
            "No container found for type parameter '${getTypeParameterName(typeParameter)}' of '${renderDeclaration(parent)}'"
        }
        appendSignature(containerIndex)
        appendSignature(MangleConstant.INDEX_SEPARATOR)
        appendSignature(getIndexOfTypeParameter(typeParameter, parent))
    }


    protected fun mangleTypeParameter(
        tpBuilder: StringBuilder,
        param: TypeParameter,
        index: Int,
        declarationSiteSession: Session
    ) = with(getTypeSystemContext(declarationSiteSession)) {
        tpBuilder.appendSignature(index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.getUpperBounds().collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) {
            @Suppress("UNCHECKED_CAST")
            mangleType(this, it as Type, declarationSiteSession)
        }
    }

    protected abstract fun getEffectiveParent(typeParameter: TypeParameter): TypeParameterContainer

    protected fun mangleValueParameter(vpBuilder: StringBuilder, param: ValueParameter, declarationSiteSession: Session) {
        mangleType(vpBuilder, getValueParameterType(param), declarationSiteSession)

        if (isVararg(param)) {
            vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
        }
    }

    protected fun mangleTypeArguments(tBuilder: StringBuilder, type: Type, declarationSiteSession: Session) =
        with(getTypeSystemContext(declarationSiteSession)) {
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
                        mangleType(this, typeArgument.getType() as Type, declarationSiteSession)
                    }
                }
            }
        }
}
