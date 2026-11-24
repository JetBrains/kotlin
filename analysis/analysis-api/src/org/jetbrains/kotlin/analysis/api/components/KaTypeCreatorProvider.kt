/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFunctionTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder

/**
 * Provides an instance of [KaTypeCreator] to create various [types][org.jetbrains.kotlin.analysis.api.types.KaType].
 */
@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaTypeCreatorProvider : KaLifetimeOwner {
    /**
     * A single entry point for the type building infrastructure.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.types.typeCreation.typeCreator' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "typeCreator",
            imports = [
                "org.jetbrains.kotlin.analysis.api.types.typeCreation.typeCreator",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public val typeCreator: KaTypeCreator

    /**
     * Creates a copy of this [KaClassType] with modifications applied via the [init] block.
     *
     * The builder is pre-populated with the properties of the original type.
     * The [init] block can then selectively override these properties.
     *
     * #### Example:
     *
     * ```kotlin
     * val nullableListOfString = listOfStringType.copy {
     *     isMarkedNullable = true
     * }
     * ```
     *
     * @see KaTypeCreator.classType
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.types.typeCreation.copy' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.copy(init)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.types.typeCreation.copy",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun <T : KaClassType> T.copy(init: KaClassTypeBuilder.() -> Unit): KaClassType

    /**
     * Creates a copy of this [KaUsualClassType] with modifications applied via the [init] block.
     *
     * The builder is pre-populated with the properties of the original type.
     * The [init] block can then selectively override these properties.
     *
     * #### Example:
     *
     * ```kotlin
     * val nullableListOfString = listOfStringType.copy {
     *     isMarkedNullable = true
     * }
     * ```
     *
     * @see KaTypeCreator.classType
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.types.typeCreation.copy' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.copy(init)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.types.typeCreation.copy",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun KaUsualClassType.copy(init: KaClassTypeBuilder.() -> Unit): KaUsualClassType

    /**
     * Creates a copy of this [KaFunctionType] with modifications applied via the [init] block.
     *
     * The builder is pre-populated with the properties of the original type.
     * The [init] block can then selectively override these properties.
     *
     * #### Example:
     *
     * ```kotlin
     * val suspendVersion = functionType.copy {
     *     isSuspend = true
     * }
     * ```
     *
     * Returns a [KaErrorType][org.jetbrains.kotlin.analysis.api.types.KaErrorType] if the arity of the function type, i.e., the
     * total number of its context parameters, receiver, and value parameters, exceeds the maximum arity of its function type
     * kind (see [KaFunctionTypeFamily.maxArity][org.jetbrains.kotlin.analysis.api.types.KaFunctionTypeFamily.maxArity]).
     *
     * @see KaTypeCreator.functionType
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.types.typeCreation.copy' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.copy(init)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.types.typeCreation.copy",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun KaFunctionType.copy(init: KaFunctionTypeBuilder.() -> Unit): KaType

    /**
     * Creates a copy of this [KaTypeParameterType] with modifications applied via the [init] block.
     *
     * The builder is pre-populated with the properties of the original type.
     * The [init] block can then selectively override these properties.
     *
     * #### Example:
     *
     * ```kotlin
     * val nullableT = typeParameterType.copy {
     *     isMarkedNullable = true
     * }
     * ```
     *
     * @see KaTypeCreator.typeParameterType
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.types.typeCreation.copy' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.copy(init)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.types.typeCreation.copy",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun KaTypeParameterType.copy(init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType
}
