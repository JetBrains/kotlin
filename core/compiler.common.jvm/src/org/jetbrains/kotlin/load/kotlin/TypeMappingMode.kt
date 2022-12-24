/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.types.Variance

@RequiresOptIn
annotation class TypeMappingModeInternals

@OptIn(TypeMappingModeInternals::class)
class TypeMappingMode @TypeMappingModeInternals constructor(
    val needPrimitiveBoxing: Boolean = true,
    val needInlineClassWrapping: Boolean = true,
    val isForAnnotationParameter: Boolean = false,
    // Here DeclarationSiteWildcards means wildcard generated because of declaration-site variance
    val skipDeclarationSiteWildcards: Boolean = false,
    val skipDeclarationSiteWildcardsIfPossible: Boolean = false,
    private val genericArgumentMode: TypeMappingMode? = null,
    val kotlinCollectionsToJavaCollections: Boolean = true,
    private val genericContravariantArgumentMode: TypeMappingMode? = genericArgumentMode,
    private val genericInvariantArgumentMode: TypeMappingMode? = genericArgumentMode,
    val mapTypeAliases: Boolean = false
) {
    companion object {
        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         */
        @JvmField
        val GENERIC_ARGUMENT = TypeMappingMode()

        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         * Type aliases are mapped to their expanded form
         */
        @JvmField
        val GENERIC_ARGUMENT_UAST = TypeMappingMode(mapTypeAliases = true)

        /**
         * see KotlinTypeMapper.forceBoxedReturnType()
         * This configuration should be called only for method return type
         */
        @JvmField
        val RETURN_TYPE_BOXED = TypeMappingMode(needInlineClassWrapping = true)

        /**
         * kotlin.Int is mapped to I
         */
        @JvmField
        val DEFAULT = TypeMappingMode(genericArgumentMode = GENERIC_ARGUMENT, needPrimitiveBoxing = false, needInlineClassWrapping = false)

        /**
         * kotlin.Int is mapped to I
         * Type aliases are mapped to their expanded form
         */
        @JvmField
        val DEFAULT_UAST = TypeMappingMode(
            genericArgumentMode = GENERIC_ARGUMENT_UAST,
            needPrimitiveBoxing = false,
            needInlineClassWrapping = false,
            mapTypeAliases = true
        )

        /**
         * kotlin.Int is mapped to I
         * inline class Foo(val x: Int) is mapped to LFoo;
         * but in signature fun bar(f: Foo), Foo is mapped to I
         */
        @JvmField
        val CLASS_DECLARATION = TypeMappingMode(
            genericArgumentMode = GENERIC_ARGUMENT,
            needPrimitiveBoxing = false,
            needInlineClassWrapping = true
        )

        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         * No projections allowed in immediate arguments
         */
        @JvmField
        val SUPER_TYPE = TypeMappingMode(skipDeclarationSiteWildcards = true, genericArgumentMode = GENERIC_ARGUMENT)

        @JvmField
        val SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS = TypeMappingMode(
            skipDeclarationSiteWildcards = true,
            genericArgumentMode = GENERIC_ARGUMENT,
            kotlinCollectionsToJavaCollections = false
        )

        /**
         * kotlin.reflect.KClass mapped to java.lang.Class when at top level or in an array;
         * primitive types and inline class types are not boxed because types in annotations cannot be nullable
         * Other types mapped as DEFAULT
         */
        @JvmField
        val VALUE_FOR_ANNOTATION = TypeMappingMode(
            isForAnnotationParameter = true,
            needPrimitiveBoxing = false,
            needInlineClassWrapping = false,
            genericArgumentMode = GENERIC_ARGUMENT
        )


        @JvmStatic
        fun getModeForReturnTypeNoGeneric(
            isAnnotationMethod: Boolean
        ) = if (isAnnotationMethod) VALUE_FOR_ANNOTATION else DEFAULT

        @JvmStatic
        fun createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards: Boolean,
            isForAnnotationParameter: Boolean,
            needInlineClassWrapping: Boolean,
            mapTypeAliases: Boolean,
            fallbackMode: TypeMappingMode? = null
        ): TypeMappingMode = TypeMappingMode(
            isForAnnotationParameter = isForAnnotationParameter,
            skipDeclarationSiteWildcards = skipDeclarationSiteWildcards,
            genericArgumentMode = fallbackMode,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases
        )
    }

    fun toGenericArgumentMode(effectiveVariance: Variance, ofArray: Boolean = false): TypeMappingMode =
        if (ofArray && isForAnnotationParameter) this else when (effectiveVariance) {
            Variance.IN_VARIANCE -> genericContravariantArgumentMode ?: this
            Variance.INVARIANT -> genericInvariantArgumentMode ?: this
            else -> genericArgumentMode ?: this
        }

    fun wrapInlineClassesMode(): TypeMappingMode =
        TypeMappingMode(
            needPrimitiveBoxing, true, isForAnnotationParameter, skipDeclarationSiteWildcards, skipDeclarationSiteWildcardsIfPossible,
            genericArgumentMode, kotlinCollectionsToJavaCollections, genericContravariantArgumentMode, genericInvariantArgumentMode
        )

    fun dontWrapInlineClassesMode(): TypeMappingMode =
        TypeMappingMode(
            needPrimitiveBoxing, false, isForAnnotationParameter, skipDeclarationSiteWildcards, skipDeclarationSiteWildcardsIfPossible,
            genericArgumentMode, kotlinCollectionsToJavaCollections, genericContravariantArgumentMode, genericInvariantArgumentMode
        )

    fun mapTypeAliases(
        genericArgumentMode: TypeMappingMode? = null
    ): TypeMappingMode =
        TypeMappingMode(
            needPrimitiveBoxing,
            needInlineClassWrapping,
            isForAnnotationParameter,
            skipDeclarationSiteWildcards,
            skipDeclarationSiteWildcardsIfPossible,
            genericArgumentMode ?: this.genericArgumentMode,
            kotlinCollectionsToJavaCollections,
            genericContravariantArgumentMode,
            genericInvariantArgumentMode,
            mapTypeAliases = true
        )
}
