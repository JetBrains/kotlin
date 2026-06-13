/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaCapturedTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaDefinitelyNotNullTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFlexibleTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaFunctionTypeBuilder
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeParameterTypeBuilder

/**
 * Provides an instance of [KaTypeCreator] to create various [types][org.jetbrains.kotlin.analysis.api.types.KaType].
 */
@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaTypeCreatorProvider : KaSessionComponent {
    /**
     * A single entry point for the type building infrastructure.
     */
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
     * @see KaTypeCreator.functionType
     */
    @KaExperimentalApi
    public fun KaFunctionType.copy(init: KaFunctionTypeBuilder.() -> Unit): KaFunctionType

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
    @KaExperimentalApi
    public fun KaTypeParameterType.copy(init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType
}

/**
 * A single entry point for the type building infrastructure.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public val typeCreator: KaTypeCreator
    get() = with(session) { typeCreator }

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
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun <T : KaClassType> T.copy(init: KaClassTypeBuilder.() -> Unit): KaClassType {
    return with(session) {
        copy(
            init = init,
        )
    }
}

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
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KaUsualClassType.copy(init: KaClassTypeBuilder.() -> Unit): KaUsualClassType {
    return with(session) {
        copy(
            init = init,
        )
    }
}

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
 * @see KaTypeCreator.functionType
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KaFunctionType.copy(init: KaFunctionTypeBuilder.() -> Unit): KaFunctionType {
    return with(session) {
        copy(
            init = init,
        )
    }
}

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
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KaTypeParameterType.copy(init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType {
    return with(session) {
        copy(
            init = init,
        )
    }
}
