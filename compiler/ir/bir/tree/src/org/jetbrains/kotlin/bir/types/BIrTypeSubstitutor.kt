/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.utils.buildSimpleType
import org.jetbrains.kotlin.bir.types.utils.makeNullable
import org.jetbrains.kotlin.bir.types.utils.toBuilder
import org.jetbrains.kotlin.bir.util.render
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.isMarkedNullable
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.utils.memoryOptimizedMap

abstract class AbstractBirTypeSubstitutor(private val birBuiltIns: BirBuiltIns) : TypeSubstitutorMarker {
    private fun BirType.typeParameterConstructor(): BirTypeParameterSymbol? {
        return if (this is BirSimpleType) classifier as? BirTypeParameterSymbol
        else null
    }

    abstract fun getSubstitutionArgument(typeParameter: BirTypeParameterSymbol): BirTypeArgument

    abstract fun isEmptySubstitution(): Boolean

    fun substitute(type: BirType): BirType {
        if (isEmptySubstitution()) return type

        return type.typeParameterConstructor()?.let {
            when (val typeArgument = getSubstitutionArgument(it)) {
                is BirStarProjection -> birBuiltIns.anyNType // TODO upper bound for T
                is BirTypeProjection -> typeArgument.type.run { if (type.isMarkedNullable()) makeNullable() else this }
            }
        } ?: substituteType(type)
    }

    private fun substituteType(irType: BirType): BirType {
        return when (irType) {
            is BirSimpleType ->
                with(irType.toBuilder()) {
                    arguments = irType.arguments.memoryOptimizedMap { substituteTypeArgument(it) }
                    buildSimpleType()
                }
            is BirDynamicType,
            is BirErrorType ->
                irType
            else ->
                throw AssertionError("Unexpected type: $irType")
        }
    }

    private fun substituteTypeArgument(typeArgument: BirTypeArgument): BirTypeArgument {
        when (typeArgument) {
            is BirStarProjection -> return typeArgument
            is BirTypeProjection -> {
                val type = typeArgument.type
                if (type is BirSimpleType) {
                    val classifier = type.classifier
                    if (classifier is BirTypeParameterSymbol) {
                        val newArgument = getSubstitutionArgument(classifier)
                        return if (newArgument is BirTypeProjection) {
                            makeTypeProjection(newArgument.type, typeArgument.variance)
                        } else newArgument
                    }
                }
                return makeTypeProjection(substituteType(typeArgument.type), typeArgument.variance)
            }
        }
    }
}

class BirTypeSubstitutor(
    typeParameters: List<BirTypeParameterSymbol>,
    typeArguments: List<BirTypeArgument>,
    irBuiltIns: BirBuiltIns
) : AbstractBirTypeSubstitutor(irBuiltIns) {

    init {
        assert(typeParameters.size == typeArguments.size) {
            "Unexpected number of type arguments: ${typeArguments.size}\n" +
                    "Type parameters are:\n" +
                    typeParameters.joinToString(separator = "\n") { it.owner.render() } +
                    "Type arguments are:\n" +
                    typeArguments.joinToString(separator = "\n") { it.render() }
        }
    }

    private val substitution = typeParameters.zip(typeArguments).toMap()

    override fun getSubstitutionArgument(typeParameter: BirTypeParameterSymbol): BirTypeArgument =
        substitution[typeParameter]
            ?: throw AssertionError("Unsubstituted type parameter: ${typeParameter.owner.render()}")

    override fun isEmptySubstitution(): Boolean = substitution.isEmpty()
}

internal class BirCapturedTypeSubstitutor(
    typeParameters: List<BirTypeParameterSymbol>,
    typeArguments: List<BirTypeArgument>,
    capturedTypes: List<BirCapturedType?>,
    irBuiltIns: BirBuiltIns
) : AbstractBirTypeSubstitutor(irBuiltIns) {

    init {
        assert(typeArguments.size == typeParameters.size)
        assert(capturedTypes.size == typeParameters.size)
    }

    private val oldSubstitution = typeParameters.zip(typeArguments).toMap()
    private val capturedSubstitution = typeParameters.zip(capturedTypes).toMap()

    override fun getSubstitutionArgument(typeParameter: BirTypeParameterSymbol): BirTypeArgument {
        return capturedSubstitution[typeParameter]?.let { makeTypeProjection(it, Variance.INVARIANT) }
            ?: oldSubstitution[typeParameter]
            ?: throw AssertionError("Unsubstituted type parameter: ${typeParameter.owner.render()}")
    }

    override fun isEmptySubstitution(): Boolean = oldSubstitution.isEmpty()
}
