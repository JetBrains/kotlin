/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.arguments
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import kotlin.reflect.KVariance

private inline fun InstructionAdapter.unrollArrayIfFewerThan(n: Int, limit: Int, type: Type, element: (Int) -> Unit): Array<Type> {
    if (n < limit) {
        return Array(n) { i ->
            element(i)
            type
        }
    }
    iconst(n)
    newarray(type)
    for (i in 0 until n) {
        dup()
        iconst(i)
        element(i)
        astore(type)
    }
    return arrayOf(AsmUtil.getArrayType(type))
}

/**
 * Returns the number of local slots used by the emitted bytecode.
 */
fun TypeSystemCommonBackendContext.generateTypeOf(
    v: InstructionAdapter,
    type: IrType,
    intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport,
    localsOffset: Int,
): Int {
    val repeatedNonReifiedTypeParameters = collectRepeatedNonReifiedTypeParameters(type)
    val builder = TypeOfBuilder(v, repeatedNonReifiedTypeParameters, localsOffset, intrinsicsSupport, this)
    builder.generateRepeatedNonReifiedTypeParameters()
    builder.generateTypeOf(type, isTypeParameterBound = false)
    return repeatedNonReifiedTypeParameters.size
}

private class TypeOfBuilder(
    val v: InstructionAdapter,
    val repeatedNonReifiedTypeParameters: List<IrTypeParameter>,
    val localsOffset: Int,
    val intrinsicsSupport: ReifiedTypeInliner.IntrinsicsSupport,
    val typeSystemContext: TypeSystemCommonBackendContext,
) {
    fun generateRepeatedNonReifiedTypeParameters() {
        for ([i, typeParameter] in repeatedNonReifiedTypeParameters.withIndex()) {
            generateNonReifiedTypeParameterWithoutUpperBounds(typeParameter)
            v.store(localsOffset + i, K_TYPE_PARAMETER)
        }
        for ([i, typeParameter] in repeatedNonReifiedTypeParameters.withIndex()) {
            if (typeParameter.superTypes.isNotEmpty()) {
                v.load(localsOffset + i, K_TYPE_PARAMETER)
                generateSetUpperBounds(typeParameter)
            }
        }
    }

    fun generateTypeOf(type: IrType, isTypeParameterBound: Boolean) {
        val classifier = type.classifierOrNull
        val typeParameter = (classifier as? IrTypeParameterSymbol)?.owner
        val methodArguments = if (typeParameter == null) {
            intrinsicsSupport.putClassInstance(v, type)
            val argumentsValues = type.arguments.orEmpty()
            val arguments = v.unrollArrayIfFewerThan(argumentsValues.size, 3, K_TYPE_PROJECTION) { i ->
                generateTypeOfArgument(argumentsValues[i], isTypeParameterBound)
            }
            arrayOf(JAVA_CLASS_TYPE, *arguments)
        } else if (!isTypeParameterBound && typeParameter.isReified) {
            val argument = ReificationArgument(typeParameter.name.asString(), type.isMarkedNullableWithContext(), 0)
            ReifiedTypeInliner.putReifiedOperationMarker(ReifiedTypeInliner.OperationKind.TYPE_OF, argument, v)
            v.aconst(null)
            return
        } else if (
            !intrinsicsSupport.config.languageVersionSettings.supportsFeature(LanguageFeature.JvmSupportRecursiveTypeOf) &&
            typeReferencesParameterWithRecursiveBound(type)
        ) {
            intrinsicsSupport.reportNonReifiedTypeParameterWithRecursiveBoundUnsupported(typeParameter.name)
            v.aconst(null)
            return
        } else {
            when (val cachedIndex = repeatedNonReifiedTypeParameters.indexOf(typeParameter)) {
                -1 -> generateNonReifiedTypeParameterWithUpperBounds(typeParameter)
                else -> v.load(localsOffset + cachedIndex, K_TYPE_PARAMETER)
            }
            arrayOf(K_CLASSIFIER_TYPE)
        }

        val methodName = if (type.isMarkedNullableWithContext()) "nullableTypeOf" else "typeOf"
        val signature = Type.getMethodDescriptor(K_TYPE, *methodArguments)
        v.invokestatic(REFLECTION, methodName, signature, false)

        if (type.isSuspendFunction()) {
            intrinsicsSupport.reportSuspendTypeUnsupported()
        }

        if (intrinsicsSupport.config.stableTypeOf) {
            if (intrinsicsSupport.isMutableCollectionType(type)) {
                v.invokestatic(REFLECTION, "mutableCollectionType", Type.getMethodDescriptor(K_TYPE, K_TYPE), false)
            } else if (classifier is IrClassSymbol && classifier.isClassWithFqName(StandardNames.FqNames.nothing)) {
                v.invokestatic(REFLECTION, "nothingType", Type.getMethodDescriptor(K_TYPE, K_TYPE), false)
            }

            val upperBoundIfFlexible = with(typeSystemContext) {
                if (type.isFlexible()) type.upperBoundIfFlexible() as IrType
                else null
            }

            if (upperBoundIfFlexible != null) {
                // If this is a flexible type, we've just generated its lower bound and have it on the stack.
                // Let's generate the upper bound now and call the method that takes lower and upper bound and constructs a flexible KType.
                generateTypeOf(upperBoundIfFlexible, isTypeParameterBound)

                v.invokestatic(REFLECTION, "platformType", Type.getMethodDescriptor(K_TYPE, K_TYPE, K_TYPE), false)
            }
        }
    }

    fun generateTypeOfArgument(projection: IrTypeArgument, isTypeParameterBound: Boolean) {
        when (projection) {
            is IrStarProjection -> {
                v.getstatic(K_TYPE_PROJECTION.internalName, "star", K_TYPE_PROJECTION.descriptor)
            }
            is IrTypeProjection -> {
                generateTypeOf(projection.type, isTypeParameterBound)
                val methodName = when (projection.variance) {
                    Variance.INVARIANT -> "invariant"
                    Variance.IN_VARIANCE -> "contravariant"
                    Variance.OUT_VARIANCE -> "covariant"
                }
                v.invokestatic(K_TYPE_PROJECTION.internalName, methodName, Type.getMethodDescriptor(K_TYPE_PROJECTION, K_TYPE), false)
            }
        }
    }

    fun generateNonReifiedTypeParameterWithoutUpperBounds(typeParameter: IrTypeParameter) {
        intrinsicsSupport.generateTypeParameterContainer(v, typeParameter)
        v.aconst(typeParameter.name.asString())
        val variance = when (typeParameter.variance) {
            Variance.INVARIANT -> KVariance.INVARIANT
            Variance.IN_VARIANCE -> KVariance.IN
            Variance.OUT_VARIANCE -> KVariance.OUT
        }
        v.getstatic(K_VARIANCE.internalName, variance.name, K_VARIANCE.descriptor)
        v.iconst(if (typeParameter.isReified) 1 else 0)
        v.invokestatic(
            REFLECTION, "typeParameter",
            Type.getMethodDescriptor(K_TYPE_PARAMETER, OBJECT_TYPE, JAVA_STRING_TYPE, K_VARIANCE, Type.BOOLEAN_TYPE),
            false,
        )
    }

    fun generateSetUpperBounds(typeParameter: IrTypeParameter) {
        val argumentsForBounds = v.unrollArrayIfFewerThan(typeParameter.superTypes.size, 2, K_TYPE) { i ->
            generateTypeOf(typeParameter.superTypes[i], isTypeParameterBound = true)
        }
        v.invokestatic(
            REFLECTION, "setUpperBounds", Type.getMethodDescriptor(Type.VOID_TYPE, K_TYPE_PARAMETER, *argumentsForBounds),
            false
        )
    }

    fun generateNonReifiedTypeParameterWithUpperBounds(typeParameter: IrTypeParameter) {
        generateNonReifiedTypeParameterWithoutUpperBounds(typeParameter)
        v.dup()
        generateSetUpperBounds(typeParameter)
    }

    private fun IrType.isMarkedNullableWithContext(): Boolean = with(typeSystemContext) {
        // JvmIrTypeSystemContext.isMarkedNullable also handles flexible types.
        this@isMarkedNullableWithContext.isMarkedNullable()
    }
}

private fun collectRepeatedNonReifiedTypeParameters(type: IrType): List<IrTypeParameter> {
    fun visitTypeParameters(type: IrType, isTypeParameterBound: Boolean, usages: MutableMap<IrTypeParameter, Int>) {
        when (val typeParameter = (type.classifierOrNull as? IrTypeParameterSymbol)?.owner) {
            null -> {
                for (argument in type.arguments.orEmpty()) {
                    (argument as? IrTypeProjection)?.type?.let { argumentType ->
                        visitTypeParameters(argumentType, isTypeParameterBound, usages)
                    }
                }
            }
            else -> {
                if (!typeParameter.isReified || isTypeParameterBound) {
                    val oldUsage = usages.getOrDefault(typeParameter, 0)
                    usages[typeParameter] = oldUsage + 1
                    if (oldUsage == 0) {
                        for (upperBound in typeParameter.superTypes) {
                            visitTypeParameters(upperBound, true, usages)
                        }
                    }
                }
            }
        }
    }

    val usages = mutableMapOf<IrTypeParameter, Int>()
    visitTypeParameters(type, false, usages)
    return usages.filter { it.value > 1 }.keys.toList()
}

private fun typeReferencesParameterWithRecursiveBound(
    type: IrType,
    used: MutableSet<IrTypeParameter> = linkedSetOf(),
): Boolean {
    val typeParameter = (type.classifierOrNull as? IrTypeParameterSymbol)?.owner
    if (typeParameter != null) {
        if (!used.add(typeParameter)) return true
        for (upperBound in typeParameter.superTypes) {
            if (typeReferencesParameterWithRecursiveBound(upperBound, used)) return true
        }
        used.remove(typeParameter)
    } else {
        for (argument in type.arguments.orEmpty()) {
            if (argument is IrTypeProjection && typeReferencesParameterWithRecursiveBound(argument.type, used)) return true
        }
    }
    return false
}
