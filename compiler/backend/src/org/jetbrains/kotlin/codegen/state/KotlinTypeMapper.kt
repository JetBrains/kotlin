/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.getEffectiveVariance
import org.jetbrains.kotlin.utils.addToStdlib.zipWithNulls
import org.jetbrains.org.objectweb.asm.Type

abstract class KotlinTypeMapper {
    companion object {
        @JvmStatic
        fun mapUnderlyingTypeOfInlineClassType(kotlinType: KotlinTypeMarker, typeMapper: KotlinTypeMapperBase): Type {
            val underlyingType = with(typeMapper.typeSystem) {
                kotlinType.getUnsubstitutedUnderlyingTypeInJvm()
            } ?: throw IllegalStateException("There should be underlying type for inline class type: $kotlinType")
            return typeMapper.mapTypeCommon(underlyingType, TypeMappingMode.DEFAULT)
        }

        fun TypeSystemContext.hasNothingInNonContravariantPosition(type: KotlinTypeMarker): Boolean {
            if (type.isError()) {
                // We cannot access type arguments for an unresolved type
                return false
            }

            val typeConstructor = type.typeConstructor()

            for (i in 0 until type.argumentsCount()) {
                val projection = type.getArgument(i)
                val argument = projection.getType() ?: continue

                if (argument.isNullableNothing() ||
                    argument.isNothing() && typeConstructor.getParameter(i).getVariance() != TypeVariance.IN
                ) return true
            }

            return false
        }

        fun TypeSystemCommonBackendContext.getVarianceForWildcard(
            parameter: TypeParameterMarker?, projection: TypeArgumentMarker, mode: TypeMappingMode
        ): Variance {
            val projectionKind = projection.getVariance().convertVariance()
            val parameterVariance = parameter?.getVariance()?.convertVariance() ?: Variance.INVARIANT

            if (parameterVariance == Variance.INVARIANT) {
                return projectionKind
            }

            if (mode.skipDeclarationSiteWildcards) {
                return Variance.INVARIANT
            }

            if (projectionKind == Variance.INVARIANT || projectionKind == parameterVariance) {
                val type = projection.getType()
                if (mode.skipDeclarationSiteWildcardsIfPossible && type != null) {
                    if (parameterVariance == Variance.OUT_VARIANCE && isMostPreciseCovariantArgument(type)) {
                        return Variance.INVARIANT
                    }

                    if (parameterVariance == Variance.IN_VARIANCE && isMostPreciseContravariantArgument(type)) {
                        return Variance.INVARIANT
                    }
                }
                return parameterVariance
            }

            // In<out X> = In<*>
            // Out<in X> = Out<*>
            return Variance.OUT_VARIANCE
        }

        fun TypeSystemCommonBackendContext.writeGenericArguments(
            signatureVisitor: JvmSignatureWriter,
            arguments: List<TypeArgumentMarker>,
            parameters: List<TypeParameterMarker>,
            mode: TypeMappingMode,
            mapType: (KotlinTypeMarker, JvmSignatureWriter, TypeMappingMode) -> Type
        ) {
            processGenericArguments(
                arguments,
                parameters,
                mode,
                processUnboundedWildcard = {
                    signatureVisitor.writeUnboundedWildcard()
                },
                processTypeArgument = { _, type, projectionKind, _, newMode ->
                    signatureVisitor.writeTypeArgument(projectionKind)
                    mapType(type, signatureVisitor, newMode)
                    signatureVisitor.writeTypeArgumentEnd()
                }
            )
        }

        fun TypeSystemCommonBackendContext.processGenericArguments(
            arguments: List<TypeArgumentMarker>,
            parameters: List<TypeParameterMarker>,
            mode: TypeMappingMode,
            processUnboundedWildcard: () -> Unit,
            processTypeArgument: (index: Int, type: KotlinTypeMarker, projectionKind: Variance, parameterVariance: Variance, mode: TypeMappingMode) -> Unit,
        ) {
            for ([index, pair] in parameters.zipWithNulls(arguments).withIndex()) {
                val [parameter, argument] = pair
                if (argument == null) break
                val type = argument.getType()
                if (type == null ||
                    // In<Nothing, Foo> == In<*, Foo> -> In<?, Foo>
                    type.isNothing() && parameter?.getVariance() == TypeVariance.IN
                ) {
                    processUnboundedWildcard()
                } else {
                    val argumentMode = mode.updateArgumentModeFromAnnotations(type, this)
                    val projectionKind = getVarianceForWildcard(parameter, argument, argumentMode)
                    val parameterVariance = parameter?.getVariance()?.convertVariance() ?: Variance.INVARIANT
                    val newMode = argumentMode.toGenericArgumentMode(
                        getEffectiveVariance(parameterVariance, argument.getVariance().convertVariance())
                    )
                    processTypeArgument(index, type, projectionKind, parameterVariance, newMode)
                }
            }
        }

        const val BOX_JVM_METHOD_NAME = "box" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS

        const val UNBOX_JVM_METHOD_NAME = "unbox" + JvmAbi.IMPL_SUFFIX_FOR_INLINE_CLASS_MEMBERS

        @JvmStatic
        fun TypeSystemCommonBackendContext.writeFormalTypeParameter(
            typeParameter: TypeParameterMarker,
            sw: JvmSignatureWriter,
            mapType: (KotlinTypeMarker, TypeMappingMode) -> Type
        ) {
            sw.writeFormalTypeParameter(typeParameter.getName().asString())

            sw.writeClassBound()

            for (i in 0 until typeParameter.upperBoundCount()) {
                val type = typeParameter.getUpperBound(i)
                if (type.typeConstructor().getTypeParameterClassifier() == null && !type.isInterfaceOrAnnotationClass()) {
                    mapType(type, TypeMappingMode.GENERIC_ARGUMENT)
                    break
                }
            }

            // "extends Object" is optional according to ClassFileFormat-Java5.pdf
            // but javac complaints to signature:
            // <P:>Ljava/lang/Object;
            // TODO: avoid writing java/lang/Object if interface list is not empty

            sw.writeClassBoundEnd()

            for (i in 0 until typeParameter.upperBoundCount()) {
                val type = typeParameter.getUpperBound(i)
                if (type.typeConstructor().getTypeParameterClassifier() != null || type.isInterfaceOrAnnotationClass()) {
                    sw.writeInterfaceBound()
                    mapType(type, TypeMappingMode.GENERIC_ARGUMENT)
                    sw.writeInterfaceBoundEnd()
                }
            }
        }
    }
}
