/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

internal class TypeMappingMode private constructor(
        val needPrimitiveBoxing: Boolean = false,
        val isForAnnotationParameter: Boolean = false,
        // Here DeclarationSiteWildcards means wildcard generated because of declaration-site variance
        val skipDeclarationSiteWildcards: Boolean = false,
        val skipDeclarationSiteWildcardsIfPossible: Boolean = false,
        private val genericArgumentMode: TypeMappingMode? = null,
        private val genericContravariantArgumentMode: TypeMappingMode? = genericArgumentMode
) {
    companion object {
        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         */
        @JvmField
        val GENERIC_TYPE = TypeMappingMode(needPrimitiveBoxing = true)

        /**
         * kotlin.Int is mapped to I
         */
        @JvmField
        val DEFAULT = TypeMappingMode(genericArgumentMode = GENERIC_TYPE)

        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         * No projections allowed in immediate arguments
         */
        @JvmField
        val SUPER_TYPE = TypeMappingMode(needPrimitiveBoxing = true, skipDeclarationSiteWildcards = true, genericArgumentMode = GENERIC_TYPE)

        /**
         * kotlin.reflect.KClass mapped to java.lang.Class
         * Other types mapped as DEFAULT
         */
        @JvmField
        val VALUE_FOR_ANNOTATION = TypeMappingMode(
                isForAnnotationParameter = true,
                genericArgumentMode = TypeMappingMode(isForAnnotationParameter = true, needPrimitiveBoxing = true, genericArgumentMode = GENERIC_TYPE))


        @JvmStatic
        fun getOptimalModeForValueParameter(
                type: KotlinType
        ) = getOptimalModeForSignaturePart(type, isForAnnotationParameter = false, canBeUsedInSupertypePosition = true)

        @JvmStatic
        fun getOptimalModeForReturnType(
                type: KotlinType,
                isAnnotationMethod: Boolean
        ) = getOptimalModeForSignaturePart(type, isForAnnotationParameter = isAnnotationMethod, canBeUsedInSupertypePosition = false)

        private fun getOptimalModeForSignaturePart(
                type: KotlinType,
                isForAnnotationParameter: Boolean,
                canBeUsedInSupertypePosition: Boolean
        ): TypeMappingMode {
            if (type.arguments.isEmpty()) return DEFAULT

            val contravariantArgumentMode =
                if (!canBeUsedInSupertypePosition)
                    TypeMappingMode(
                            needPrimitiveBoxing = true,
                            isForAnnotationParameter = isForAnnotationParameter,
                            skipDeclarationSiteWildcards = false,
                            skipDeclarationSiteWildcardsIfPossible = true)
                else
                    null

            return TypeMappingMode(
                    needPrimitiveBoxing = true,
                    isForAnnotationParameter = isForAnnotationParameter,
                    skipDeclarationSiteWildcards = !canBeUsedInSupertypePosition,
                    skipDeclarationSiteWildcardsIfPossible = true,
                    genericContravariantArgumentMode = contravariantArgumentMode)
        }
    }

    fun toGenericArgumentMode(effectiveVariance: Variance): TypeMappingMode =
            when (effectiveVariance) {
                Variance.IN_VARIANCE -> genericContravariantArgumentMode ?: this
                else -> genericArgumentMode ?: this
            }
}
