/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImplementationDetail

/**
 * How a value class is unboxed by the compiler, as stored in a [KotlinClassStubImpl].
 */
@KtImplementationDetail
sealed interface KotlinValueClassRepresentation {
    /**
     * The underlying properties of the value class, in declaration order.
     *
     * The list is empty only for an abstract or a sealed [KotlinFullValueClassRepresentation]: any other value class
     * has to declare at least one underlying property.
     */
    val underlyingPropertyNamesToTypes: List<Pair<Name, KotlinRigidTypeBean>>
}

/**
 * A value class declared with the `@JvmInline` annotation.
 */
@KtImplementationDetail
data class KotlinInlineClassRepresentation(
    val underlyingPropertyName: Name,
    val underlyingType: KotlinRigidTypeBean,
) : KotlinValueClassRepresentation {
    override val underlyingPropertyNamesToTypes: List<Pair<Name, KotlinRigidTypeBean>>
        get() = listOf(underlyingPropertyName to underlyingType)
}

/**
 * A value class declared without the `@JvmInline` annotation. It may have several underlying properties, and it is unboxed only on
 * non-JVM platforms and only if it has a single underlying property and no supertype other than `kotlin.Any`.
 *
 * An abstract or a sealed value class cannot declare underlying properties, so [underlyingPropertyNamesToTypes] is empty for it.
 */
@KtImplementationDetail
data class KotlinFullValueClassRepresentation(
    override val underlyingPropertyNamesToTypes: List<Pair<Name, KotlinRigidTypeBean>>,
) : KotlinValueClassRepresentation
