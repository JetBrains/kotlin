/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("KTypesJvm")

package kotlin.reflect.jvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.internal.KTypeImpl
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError

/**
 * Returns the [KClass] instance representing the runtime class to which this type is erased to on JVM.
 */
@SinceKotlin("1.1")
val KType.jvmErasure: KClass<*>
    get() = classifier?.jvmErasure ?: throw KotlinReflectionInternalError("Cannot calculate JVM erasure for type: $this")

internal val KClassifier.jvmErasure: KClass<*>
    get() = when (this) {
        is KClass<*> -> this
        is KTypeParameter -> {
            // See getRepresentativeUpperBound in typeSignatureMapping.kt
            val bounds = upperBounds
            val representativeBound = bounds.firstOrNull {
                val classDescriptor = (it as KTypeImpl).type.constructor.declarationDescriptor as? ClassDescriptor
                classDescriptor != null && classDescriptor.kind != INTERFACE && classDescriptor.kind != ANNOTATION_CLASS
            } ?: bounds.firstOrNull()
            representativeBound?.jvmErasure ?: Any::class
        }
        else -> throw KotlinReflectionInternalError("Cannot calculate JVM erasure for type: $this")
    }
