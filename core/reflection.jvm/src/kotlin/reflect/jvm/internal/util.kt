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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.load.java.components.RuntimeSourceElementFactory
import org.jetbrains.kotlin.load.java.reflect.tryLoadClass
import org.jetbrains.kotlin.load.java.structure.reflect.ReflectJavaAnnotation
import org.jetbrains.kotlin.load.java.structure.reflect.ReflectJavaClass
import org.jetbrains.kotlin.load.java.structure.reflect.safeClassLoader
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectAnnotationSource
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import kotlin.jvm.internal.FunctionReference
import kotlin.jvm.internal.PropertyReference
import kotlin.reflect.IllegalCallableAccessException
import kotlin.reflect.KVisibility

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
            // If this is neither a Kotlin class nor a Java class, it is either a built-in or some fake class descriptor like the one
            // that's created for java.io.Serializable in JvmBuiltInsSettings
            val classId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(DescriptorUtils.getFqName(this)) ?: classId
            val packageName = classId.packageFqName.asString()
            val className = classId.relativeClassName.asString()
            // All pseudo-classes like kotlin.String.Companion must be accessible from the current class loader
            loadClass(javaClass.safeClassLoader, packageName, className)
        }
    }
}

internal fun loadClass(classLoader: ClassLoader, packageName: String, className: String): Class<*>? {
    if (packageName == "kotlin") {
        // See mapBuiltInType() in typeSignatureMapping.kt
        when (className) {
            "Array" -> return Array<Any>::class.java
            "BooleanArray" -> return BooleanArray::class.java
            "ByteArray" -> return ByteArray::class.java
            "CharArray" -> return CharArray::class.java
            "DoubleArray" -> return DoubleArray::class.java
            "FloatArray" -> return FloatArray::class.java
            "IntArray" -> return IntArray::class.java
            "LongArray" -> return LongArray::class.java
            "ShortArray" -> return ShortArray::class.java
        }
    }

    return classLoader.tryLoadClass("$packageName.${className.replace('.', '$')}")
}

internal fun Visibility.toKVisibility(): KVisibility? =
        when (this) {
            Visibilities.PUBLIC -> KVisibility.PUBLIC
            Visibilities.PROTECTED -> KVisibility.PROTECTED
            Visibilities.INTERNAL -> KVisibility.INTERNAL
            Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS -> KVisibility.PRIVATE
            else -> null
        }

internal fun Annotated.computeAnnotations(): List<Annotation> =
        annotations.mapNotNull {
            val source = it.source
            when (source) {
                is ReflectAnnotationSource -> source.annotation
                is RuntimeSourceElementFactory.RuntimeSourceElement -> (source.javaElement as? ReflectJavaAnnotation)?.annotation
                else -> null
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
