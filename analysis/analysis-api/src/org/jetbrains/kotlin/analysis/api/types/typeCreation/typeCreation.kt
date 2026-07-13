/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType

/**
 * A single entry point for the type building infrastructure.
 */
@KaExperimentalApi
context(session: KaSession)
public val typeCreator: KaTypeCreator
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeCreatorProvider.typeCreator
    }

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
context(session: KaSession)
public fun <T : KaClassType> T.copy(init: KaClassTypeBuilder.() -> Unit): KaClassType {
    @OptIn(KaImplementationDetail::class)
    return internals.typeCreatorProvider.copy(this, init)
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
@KaExperimentalApi
context(session: KaSession)
public fun KaUsualClassType.copy(init: KaClassTypeBuilder.() -> Unit): KaUsualClassType {
    @OptIn(KaImplementationDetail::class)
    return internals.typeCreatorProvider.copy(this, init)
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
@KaExperimentalApi
context(session: KaSession)
public fun KaFunctionType.copy(init: KaFunctionTypeBuilder.() -> Unit): KaFunctionType {
    @OptIn(KaImplementationDetail::class)
    return internals.typeCreatorProvider.copy(this, init)
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
@KaExperimentalApi
context(session: KaSession)
public fun KaTypeParameterType.copy(init: KaTypeParameterTypeBuilder.() -> Unit): KaTypeParameterType {
    @OptIn(KaImplementationDetail::class)
    return internals.typeCreatorProvider.copy(this, init)
}
