/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.types.Variance

@RequiresOptIn
annotation class TypeMappingModeInternals

@OptIn(TypeMappingModeInternals::class)
data class TypeMappingMode @TypeMappingModeInternals constructor(
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
    val mapTypeAliases: Boolean = false,
    val ignoreTypeArgumentsBounds: Boolean = false,
) {
    companion object {
        /**
         * kotlin.Int is mapped to Ljava/lang/Integer;
         */
        @JvmField
        val GENERIC_ARGUMENT = TypeMappingMode()

        /**
         * @see GENERIC_ARGUMENT
         * @see SUPER_TYPE_AS_IS
         * @see SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
         */
        @JvmField
        val GENERIC_ARGUMENT_FOR_SUPER_TYPES_AS_IS = TypeMappingMode(ignoreTypeArgumentsBounds = true)

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


        /**
         * The same as [SUPER_TYPE], but without type arguments bounds checks
         *
         * @see SUPER_TYPE
         * */
        @JvmField
        val SUPER_TYPE_AS_IS = TypeMappingMode(
            skipDeclarationSiteWildcards = true,
            genericArgumentMode = GENERIC_ARGUMENT_FOR_SUPER_TYPES_AS_IS,
            ignoreTypeArgumentsBounds = true,
        )

        @JvmField
        val SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS = TypeMappingMode(
            skipDeclarationSiteWildcards = true,
            genericArgumentMode = GENERIC_ARGUMENT_FOR_SUPER_TYPES_AS_IS,
            kotlinCollectionsToJavaCollections = false,
            ignoreTypeArgumentsBounds = true,
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

        /**
         * Used to map Kotlin type to Java classes passed as bootstrap method arguments.
         *
         * Primitive types and inline classes are boxed, Kotlin colelctions are mapped to Java collections;
         * the generic types can only be used with the star projection (`<*>`).
         */
        @JvmField
        val INVOKE_DYNAMIC_BOOTSTRAP_ARGUMENT = TypeMappingMode(
            needPrimitiveBoxing = true,
            needInlineClassWrapping = true,
            kotlinCollectionsToJavaCollections = true
        )

        @JvmStatic
        fun getModeForReturnTypeNoGeneric(
            isAnnotationMethod: Boolean
        ): TypeMappingMode = if (isAnnotationMethod) VALUE_FOR_ANNOTATION else DEFAULT

        @JvmStatic
        fun createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards: Boolean,
            isForAnnotationParameter: Boolean,
            needInlineClassWrapping: Boolean,
            mapTypeAliases: Boolean,
            ignoreTypeArgumentsBounds: Boolean = false,
            fallbackMode: TypeMappingMode? = null
        ): TypeMappingMode = TypeMappingMode(
            isForAnnotationParameter = isForAnnotationParameter,
            skipDeclarationSiteWildcards = skipDeclarationSiteWildcards,
            genericArgumentMode = fallbackMode,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases,
            ignoreTypeArgumentsBounds = ignoreTypeArgumentsBounds,
        )
    }

    fun toGenericArgumentMode(effectiveVariance: Variance, ofArray: Boolean = false): TypeMappingMode =
        if (ofArray && isForAnnotationParameter) this else when (effectiveVariance) {
            Variance.IN_VARIANCE -> genericContravariantArgumentMode ?: this
            Variance.INVARIANT -> genericInvariantArgumentMode ?: this
            else -> genericArgumentMode ?: this
        }

    fun wrapInlineClassesMode(): TypeMappingMode = TypeMappingMode(
        needPrimitiveBoxing = needPrimitiveBoxing,
        needInlineClassWrapping = true,
        isForAnnotationParameter = isForAnnotationParameter,
        skipDeclarationSiteWildcards = skipDeclarationSiteWildcards,
        skipDeclarationSiteWildcardsIfPossible = skipDeclarationSiteWildcardsIfPossible,
        genericArgumentMode = genericArgumentMode,
        kotlinCollectionsToJavaCollections = kotlinCollectionsToJavaCollections,
        genericContravariantArgumentMode = genericContravariantArgumentMode,
        genericInvariantArgumentMode = genericInvariantArgumentMode,
        mapTypeAliases = mapTypeAliases,
        ignoreTypeArgumentsBounds = ignoreTypeArgumentsBounds,
    )

    fun dontWrapInlineClassesMode(): TypeMappingMode = TypeMappingMode(
        needPrimitiveBoxing = needPrimitiveBoxing,
        needInlineClassWrapping = false,
        isForAnnotationParameter = isForAnnotationParameter,
        skipDeclarationSiteWildcards = skipDeclarationSiteWildcards,
        skipDeclarationSiteWildcardsIfPossible = skipDeclarationSiteWildcardsIfPossible,
        genericArgumentMode = genericArgumentMode,
        kotlinCollectionsToJavaCollections = kotlinCollectionsToJavaCollections,
        genericContravariantArgumentMode = genericContravariantArgumentMode,
        genericInvariantArgumentMode = genericInvariantArgumentMode,
        mapTypeAliases = mapTypeAliases,
        ignoreTypeArgumentsBounds = ignoreTypeArgumentsBounds,
    )

    fun mapTypeAliases(
        genericArgumentMode: TypeMappingMode? = null,
    ): TypeMappingMode = TypeMappingMode(
        needPrimitiveBoxing = needPrimitiveBoxing,
        needInlineClassWrapping = needInlineClassWrapping,
        isForAnnotationParameter = isForAnnotationParameter,
        skipDeclarationSiteWildcards = skipDeclarationSiteWildcards,
        skipDeclarationSiteWildcardsIfPossible = skipDeclarationSiteWildcardsIfPossible,
        genericArgumentMode = genericArgumentMode ?: this.genericArgumentMode,
        kotlinCollectionsToJavaCollections = kotlinCollectionsToJavaCollections,
        genericContravariantArgumentMode = genericContravariantArgumentMode,
        genericInvariantArgumentMode = genericInvariantArgumentMode,
        mapTypeAliases = true,
        ignoreTypeArgumentsBounds = ignoreTypeArgumentsBounds,
    )
}
