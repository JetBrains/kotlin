/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

inline fun <reified T : Any> descriptorOf(): TypeDescriptor =
    TypeDescriptor(
        properties = T::class.memberProperties.map { prop ->
            Property(
                name = prop.name,
                type = prop.returnType.classifier
                    ?.let { (it as? KClass<*>)?.qualifiedName }
                    ?: prop.returnType.toString(),
                isNullable = prop.returnType.isMarkedNullable,
            )
        }
    )

data class TypeDescriptor(
    val properties: List<Property>,
)

@Serializable
data class Property(
    val name: String,
    val type: String,
    val isNullable: Boolean = false,
)