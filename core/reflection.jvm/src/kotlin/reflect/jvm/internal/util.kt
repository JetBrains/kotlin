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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.load.java.components.RuntimeSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.reflect.ReflectJavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.name.FqName
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.PropertyReference
import kotlin.reflect.IllegalCallableAccessException

internal val JVM_STATIC = FqName("kotlin.jvm.JvmStatic")

internal fun ClassDescriptor.toJavaClass(): Class<*>? {
    val source = source
    return when (source) {
        is KotlinJvmBinarySourceElement -> {
            (source.binaryClass as ReflectKotlinClass).klass
        }
        is RuntimeSourceElementFactory.RuntimeSourceElement -> {
            (source.javaElement as ReflectJavaClass).element
        }
        else -> {
            // This is a built-in class
            null
        }
    }
}

// TODO: wrap other exceptions
internal inline fun <R> reflectionCall(block: () -> R): R =
        try {
            block()
        }
        catch (e: IllegalAccessException) {
            throw IllegalCallableAccessException(e)
        }

internal fun Any?.asKFunctionImpl(): KFunctionImpl? =
        this as? KFunctionImpl ?:
        (this as? FunctionReference)?.compute() as? KFunctionImpl

internal fun Any?.asKPropertyImpl(): KPropertyImpl<*>? =
        this as? KPropertyImpl<*> ?:
        (this as? PropertyReference)?.compute() as? KPropertyImpl

internal fun Any?.asKCallableImpl(): KCallableImpl<*>? =
        this as? KCallableImpl<*> ?: asKFunctionImpl() ?: asKPropertyImpl()
