/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtImplementationDetail

/**
 * The kind of a value class together with its underlying properties, as stored in a [KotlinClassStubImpl].
 */
@KtImplementationDetail
sealed interface KotlinValueClassRepresentation {
    /**
     * The underlying properties of the value class, in declaration order.
     *
     * It is `null` for an abstract or a sealed [KotlinFullValueClassRepresentation], which is not allowed to declare them.
     * Any other value class has to declare at least one underlying property.
     */
    val underlyingPropertyNamesToTypes: List<Pair<Name, KotlinRigidTypeBean>>?
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
 * A value class declared without the `@JvmInline` annotation.
 */
@KtImplementationDetail
data class KotlinFullValueClassRepresentation(
    override val underlyingPropertyNamesToTypes: List<Pair<Name, KotlinRigidTypeBean>>?,
) : KotlinValueClassRepresentation
