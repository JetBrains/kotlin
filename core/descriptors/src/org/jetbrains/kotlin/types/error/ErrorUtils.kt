/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.contains

object ErrorUtils {
    val errorModule: ModuleDescriptor = ErrorModuleDescriptor
    val errorClass: ErrorClassDescriptor = ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format("unknown class")))

    // Do not move it into AbstractTypeConstructor.Companion because of cycle in initialization(see KT-13264)
    val errorTypeForLoopInSupertypes: KotlinType = createErrorType(ErrorTypeKind.CYCLIC_SUPERTYPES)
    val errorPropertyType: KotlinType = createErrorType(ErrorTypeKind.ERROR_PROPERTY_TYPE)

    private val errorProperty: PropertyDescriptor = ErrorPropertyDescriptor()
    val errorPropertyGroup: Set<PropertyDescriptor> = setOf(errorProperty)

    /**
     * @return true if any of the types referenced in parameter types (including type parameters and extension receiver) of the function
     * is an error type. Does not check the return type of the function.
     */
    fun containsErrorTypeInParameters(function: FunctionDescriptor): Boolean {
        val receiverParameter = function.extensionReceiverParameter
        if (receiverParameter != null && containsErrorType(receiverParameter.type))
            return true

        for (parameter in function.valueParameters) {
            if (containsErrorType(parameter.type))
                return true
        }

        for (parameter in function.typeParameters) {
            for (upperBound in parameter.upperBounds) {
                if (containsErrorType(upperBound))
                    return true
            }
        }
        return false
    }

    @JvmStatic
    fun createErrorScope(kind: ErrorScopeKind, vararg formatParams: String): ErrorScope =
        createErrorScope(kind, throwExceptions = false, *formatParams)

    @JvmStatic
    fun createErrorScope(kind: ErrorScopeKind, throwExceptions: Boolean, vararg formatParams: String): ErrorScope =
        if (throwExceptions) ThrowingScope(kind, *formatParams) else ErrorScope(kind, *formatParams)

    @JvmStatic
    fun createErrorType(kind: ErrorTypeKind, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), *formatParams)

    fun createErrorType(kind: ErrorTypeKind, typeConstructor: TypeConstructor, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), typeConstructor, *formatParams)

    fun createErrorTypeWithArguments(kind: ErrorTypeKind, arguments: List<TypeProjection>, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, arguments, createErrorTypeConstructor(kind, *formatParams), *formatParams)

    fun createErrorTypeWithArguments(
        kind: ErrorTypeKind,
        arguments: List<TypeProjection>,
        typeConstructor: TypeConstructor,
        vararg formatParams: String
    ): ErrorType = ErrorType(
        typeConstructor, createErrorScope(ErrorScopeKind.ERROR_TYPE_SCOPE, typeConstructor.toString()),
        kind, arguments, isMarkedNullable = false, *formatParams
    )

    fun createErrorTypeConstructor(kind: ErrorTypeKind, vararg formatParams: String): ErrorTypeConstructor =
        ErrorTypeConstructor(kind, *formatParams)

    fun containsErrorType(type: KotlinType?): Boolean {
        if (type == null) return false
        if (type.isError) return true
        for (projection in type.arguments) {
            if (!projection.isStarProjection && containsErrorType(projection.type))
                return true
        }
        return false
    }

    @JvmStatic
    fun isError(candidate: DeclarationDescriptor?): Boolean =
        candidate != null && (isErrorClass(candidate) || isErrorClass(candidate.containingDeclaration) || candidate === errorModule)

    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor

    @JvmStatic
    fun isUninferredTypeVariable(type: KotlinType?): Boolean {
        if (type == null) return false
        val constructor = type.constructor
        return constructor is ErrorTypeConstructor && constructor.kind == ErrorTypeKind.UNINFERRED_TYPE_VARIABLE
    }

    fun containsUninferredTypeVariable(type: KotlinType): Boolean = type.contains(::isUninferredTypeVariable)
}
