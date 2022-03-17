/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.ClassicTypeSystemContext
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

internal class KtFe10TypeSystemCommonBackendContextForTypeMapping(
    private val resolveSession: ResolveSession
) : TypeSystemCommonBackendContextForTypeMapping, ClassicTypeSystemContext {
    override val builtIns: KotlinBuiltIns
        get() = resolveSession.moduleDescriptor.builtIns

    override fun TypeConstructorMarker.isTypeParameter(): Boolean {
        require(this is TypeConstructor)
        return when (this) {
            is NewTypeVariableConstructor -> originalTypeParameter != null
            else -> declarationDescriptor is TypeParameterDescriptor
        }
    }

    override fun TypeConstructorMarker.asTypeParameter(): TypeParameterMarker {
        require(this is TypeConstructor)
        return when (this) {
            is NewTypeVariableConstructor -> originalTypeParameter!!
            else -> declarationDescriptor as TypeParameterDescriptor
        }
    }

    override fun TypeConstructorMarker.defaultType(): KotlinTypeMarker {
        require(this is TypeConstructor)
        val declaration = declarationDescriptor
            ?: return ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_DECLARATION, this.toString())
        return declaration.defaultType
    }

    override fun TypeConstructorMarker.isScript(): Boolean {
        require(this is TypeConstructor)
        return declarationDescriptor is ScriptDescriptor
    }

    override fun SimpleTypeMarker.isSuspendFunction(): Boolean {
        require(this is SimpleType)
        val declaration = constructor.declarationDescriptor
        return declaration is FunctionClassDescriptor && declaration.functionKind.isSuspendType
    }

    override fun SimpleTypeMarker.isKClass(): Boolean {
        require(this is SimpleType)
        return constructor.declarationDescriptor == builtIns.kClass
    }

    override fun KotlinTypeMarker.isRawType(): Boolean {
        require(this is KotlinType)
        return when (val declaration = constructor.declarationDescriptor) {
            is ClassifierDescriptorWithTypeParameters -> declaration.declaredTypeParameters.isNotEmpty() && arguments.isEmpty()
            else -> false
        }
    }

    override fun TypeConstructorMarker.typeWithArguments(arguments: List<KotlinTypeMarker>): SimpleTypeMarker {
        require(this is TypeConstructor)
        require(parameters.size == arguments.size)

        val declaration = declarationDescriptor
        if (declaration == null) {
            val errorArguments = arguments.map { TypeProjectionImpl(it as KotlinType) }
            return ErrorUtils.createErrorTypeWithArguments(ErrorTypeKind.UNRESOLVED_TYPE, errorArguments, this.toString())
        }

        val substitutions = LinkedHashMap<TypeConstructor, TypeProjection>(parameters.size)
        for (index in parameters.indices) {
            val parameterTypeConstructor = parameters[index].typeConstructor
            substitutions[parameterTypeConstructor] = TypeProjectionImpl(arguments[index] as KotlinType)
        }

        return TypeSubstitutor.create(substitutions).substitute(declaration.defaultType, Variance.INVARIANT) as SimpleType
    }

    override fun TypeParameterMarker.representativeUpperBound(): KotlinTypeMarker {
        require(this is TypeParameterDescriptor)

        for (upperBound in upperBounds) {
            val declaration = upperBound.constructor.declarationDescriptor as? ClassDescriptor ?: continue
            if (declaration.kind != ClassKind.INTERFACE && declaration.kind != ClassKind.ANNOTATION_CLASS) {
                return upperBound
            }
        }

        return upperBounds.firstOrNull() ?: builtIns.nullableAnyType
    }

    override fun continuationTypeConstructor(): TypeConstructorMarker {
        val continuationFqName = StandardClassIds.Continuation.asSingleFqName()
        val foundClasses = resolveSession.getTopLevelClassifierDescriptors(continuationFqName, NoLookupLocation.FROM_IDE)
        return foundClasses.firstOrNull()?.typeConstructor
            ?: ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.NOT_FOUND_FQNAME, continuationFqName.toString())
    }

    override fun functionNTypeConstructor(n: Int): TypeConstructorMarker {
        return builtIns.getKFunction(n).typeConstructor
    }
}
