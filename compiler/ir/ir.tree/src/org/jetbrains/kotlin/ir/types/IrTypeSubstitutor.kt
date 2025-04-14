/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.eraseTypeParameters
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap

abstract class AbstractIrTypeSubstitutor : TypeSubstitutorMarker {
    abstract fun substitute(type: IrType): IrType

    object Empty : AbstractIrTypeSubstitutor() {
        override fun substitute(type: IrType): IrType {
            return type
        }
    }

    companion object {
        /**
         * Returns a substitutor, which would be able to substitute type parameters of [parentClass]
         * and all classes in between with arguments of [type].
         *
         * Returns null, if [type] is not subclass of [parentClass]
         */
        fun forTypeAsSuperClass(parentClass: IrClass, type: IrSimpleType): AbstractIrTypeSubstitutor? {
            if (type.classOrNull == parentClass.symbol) return forType(type)
            return createSupertypeSubstitutor(parentClass, type)?.chainedWith(forType(type))
        }

        /**
         * Returns a substitutor, which would be able to substitute type parameters of [parentClass]
         * and all classes in between with type parameters of [childClass].
         *
         * Returns null, if [childClass] is not subclass of [parentClass]
         */
        fun forSuperClass(parentClass: IrClass, childClass: IrClass): AbstractIrTypeSubstitutor? {
            if (parentClass == childClass) return Empty
            return createSupertypeSubstitutor(parentClass, childClass.defaultType)
        }

        /**
         * Returns a substitutor, which would be able to substitute type parameters
         * of [IrType::classifier] of the given type with its arguments.
         */
        fun forType(type: IrSimpleType): AbstractIrTypeSubstitutor {
            val clazz = type.classOrNull ?: return Empty
            val typeParameters = clazz.owner.typeParameters.map { it.symbol }
            /**
             * Type may be a local class that captures type parameters of outer function, so we need to take only first
             *   arguments, which correspond to type parameters of actual class declaration
             */
            val typeArgumentsForSubstitutor = type.arguments
                .take(clazz.owner.typeParameters.size)
                .mapIndexed { index, argument ->
                    when (argument) {
                        is IrStarProjection -> typeParameters[index].owner.superTypes.first()
                        else -> argument
                    }
                }

            return IrTypeSubstitutor(
                typeParameters,
                typeArgumentsForSubstitutor,
                allowEmptySubstitution = true
            )
        }
    }
}

abstract class BaseIrTypeSubstitutor : AbstractIrTypeSubstitutor() {
    abstract fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument

    abstract fun isEmptySubstitution(): Boolean

    final override fun substitute(type: IrType): IrType {
        if (isEmptySubstitution()) return type
        return when (val result = substituteType(type)) {
            is IrStarProjection -> error("Cannot replace top-level type with star projection: ${type.render()}")
            is IrTypeProjection -> result.type
        }
    }

    private fun substituteType(irType: IrType): IrTypeArgument {
        val classifier = (irType as? IrSimpleType)?.classifier
        if (classifier is IrTypeParameterSymbol) {
            return when (val typeArgument = getSubstitutionArgument(classifier)) {
                is IrStarProjection -> typeArgument
                is IrTypeProjection -> makeTypeProjection(
                    typeArgument.type.mergeNullability(irType).addAnnotations(irType.annotations),
                    typeArgument.variance,
                )
            }
        }

        return when (irType) {
            is IrSimpleType -> with(irType.toBuilder()) {
                arguments = irType.arguments.memoryOptimizedMap { substituteTypeArgument(it) }
                buildSimpleType()
            }
            is IrDynamicType, is IrErrorType -> makeTypeProjection(irType, Variance.INVARIANT)
        }
    }

    private fun substituteTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        when (typeArgument) {
            is IrStarProjection -> typeArgument
            is IrTypeProjection -> when (val replacement = substituteType(typeArgument.type)) {
                is IrStarProjection -> replacement
                is IrTypeProjection -> makeTypeProjection(
                    replacement.type, TypeSubstitutor.combine(typeArgument.variance, replacement.variance)
                )
            }
        }
}

class IrTypeSubstitutor(
    private val substitution: Map<IrTypeParameterSymbol, IrTypeArgument>,
    private val allowEmptySubstitution: Boolean = false
) : BaseIrTypeSubstitutor() {
    constructor(
        typeParameters: List<IrTypeParameterSymbol>,
        typeArguments: List<IrTypeArgument>,
        allowEmptySubstitution: Boolean = false,
    ) : this(typeParameters.zip(typeArguments).toMap(), allowEmptySubstitution) {
        check(typeParameters.size == typeArguments.size) {
            "Unexpected number of type arguments: ${typeArguments.size}\n" +
                    "Type parameters are:\n" +
                    typeParameters.joinToString(separator = "\n") { it.owner.render() } +
                    "Type arguments are:\n" +
                    typeArguments.joinToString(separator = "\n") { it.render() }
        }
    }

    override fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument =
        substitution[typeParameter]
            ?: typeParameter.takeIf { allowEmptySubstitution }?.owner?.defaultType
            ?: error("Unsubstituted type parameter: ${typeParameter.owner.render()}")

    override fun isEmptySubstitution(): Boolean = substitution.isEmpty()
}

internal class IrCapturedTypeSubstitutor(
    typeParameters: List<IrTypeParameterSymbol>,
    typeArguments: List<IrTypeArgument>,
    capturedTypes: List<IrCapturedType?>,
) : BaseIrTypeSubstitutor() {
    init {
        assert(typeArguments.size == typeParameters.size)
        assert(capturedTypes.size == typeParameters.size)
    }

    private val oldSubstitution = typeParameters.zip(typeArguments).toMap()
    private val capturedSubstitution = typeParameters.zip(capturedTypes).toMap()

    override fun getSubstitutionArgument(typeParameter: IrTypeParameterSymbol): IrTypeArgument {
        return capturedSubstitution[typeParameter]?.let { makeTypeProjection(it, Variance.INVARIANT) }
            ?: oldSubstitution[typeParameter]
            ?: throw AssertionError("Unsubstituted type parameter: ${typeParameter.owner.render()}")
    }

    override fun isEmptySubstitution(): Boolean = oldSubstitution.isEmpty()
}

class IrChainedSubstitutor(val first: AbstractIrTypeSubstitutor, val second: AbstractIrTypeSubstitutor) : AbstractIrTypeSubstitutor() {
    override fun substitute(type: IrType): IrType {
        val firstResult = first.substitute(type)
        return second.substitute(firstResult)
    }
}

fun AbstractIrTypeSubstitutor.chainedWith(other: AbstractIrTypeSubstitutor?): AbstractIrTypeSubstitutor {
    if (other == null) return this
    if (this === AbstractIrTypeSubstitutor.Empty) return other
    return IrChainedSubstitutor(this, other)
}

private fun createSupertypeSubstitutor(parentClass: IrClass, type: IrSimpleType): AbstractIrTypeSubstitutor? {
    val visited = mutableSetOf<IrClass>()

    fun find(type: IrSimpleType, targetClass: IrClass): AbstractIrTypeSubstitutor? {
        val currentClass = type.classOrFail.owner
        if (currentClass == targetClass) return AbstractIrTypeSubstitutor.Empty
        if (!visited.add(currentClass)) return null

        val superTypeSubstitutors = getImmediateSupertypes(type.classOrFail.owner)

        superTypeSubstitutors.firstNotNullOfOrNull {
            runIf(it.key.classOrFail.owner == targetClass) { it.value }
        }?.let { return it }

        for ((superType, substitutor) in superTypeSubstitutors) {
            val otherSubstitutor = find(superType, targetClass) ?: continue
            return otherSubstitutor.chainedWith(substitutor)
        }
        return null
    }

    return find(type, parentClass)
}

private fun getImmediateSupertypes(irClass: IrClass): Map<IrSimpleType, AbstractIrTypeSubstitutor> {
    @Suppress("UNCHECKED_CAST")
    val originalSupertypes = irClass.superTypes as List<IrSimpleType>
    return originalSupertypes
        .filter { it.classOrNull != null }
        .associateWith { superType ->
            val superClass = superType.classOrFail.owner
            IrTypeSubstitutor(superClass.typeParameters.map { it.symbol }, superType.arguments, allowEmptySubstitution = true)
        }
}