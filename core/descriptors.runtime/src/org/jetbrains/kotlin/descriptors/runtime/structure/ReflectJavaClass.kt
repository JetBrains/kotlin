/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.runtime.structure

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaRecordComponent
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.*

class ReflectJavaClass(
    private val klass: Class<*>
) : ReflectJavaElement(), ReflectJavaAnnotationOwner, ReflectJavaModifierListOwner, JavaClass {
    override val element: Class<*> get() = klass

    override val modifiers: Int get() = klass.modifiers

    override val isFromSource: Boolean get() = false

    override val innerClassNames: List<Name>
        get() = klass.declaredClasses
            .asSequence()
            .filterNot {
                // getDeclaredClasses() returns anonymous classes sometimes, for example enums with specialized entries (which are
                // in fact anonymous classes) or in case of a special anonymous class created for the synthetic accessor to a private
                // nested class constructor accessed from the outer class
                it.simpleName.isEmpty()
            }
            .mapNotNull { it.simpleName.takeIf(Name::isValidIdentifier)?.let(Name::identifier) }.toList()

    override fun findInnerClass(name: Name) = klass.declaredClasses
        .asSequence()
        .firstOrNull {
            it.simpleName == name.asString()
        }?.let(::ReflectJavaClass)

    override val fqName: FqName
        get() = klass.classId.asSingleFqName()

    override val outerClass: ReflectJavaClass?
        get() = klass.declaringClass?.let(::ReflectJavaClass)

    override val supertypes: Collection<JavaClassifierType>
        get() {
            if (klass == Any::class.java) return emptyList()
            return listOf(klass.genericSuperclass ?: Any::class.java, *klass.genericInterfaces).map(::ReflectJavaClassifierType)
        }

    override val methods: List<ReflectJavaMethod>
        get() = klass.declaredMethods
            .asSequence()
            .filter { method ->
                when {
                    method.isSynthetic -> false
                    isEnum -> !isEnumValuesOrValueOf(method)
                    else -> true
                }
            }
            .map(::ReflectJavaMethod)
            .toList()

    private fun isEnumValuesOrValueOf(method: Method): Boolean {
        return when (method.name) {
            "values" -> method.parameterTypes.isEmpty()
            "valueOf" -> Arrays.equals(method.parameterTypes, arrayOf(String::class.java))
            else -> false
        }
    }

    override val fields: List<ReflectJavaField>
        get() = klass.declaredFields
            .asSequence()
            .filterNot(Member::isSynthetic)
            .map(::ReflectJavaField)
            .toList()

    override val constructors: List<ReflectJavaConstructor>
        get() = klass.declaredConstructors
            .asSequence()
            .filterNot(Member::isSynthetic)
            .map(::ReflectJavaConstructor)
            .toList()

    override fun hasDefaultConstructor() = false // any default constructor is returned by constructors

    override val lightClassOriginKind: LightClassOriginKind?
        get() = null

    override val name: Name
        get() = if (klass.isAnonymousClass) {
            // For anonymous classes, `Class.simpleName` returns an empty string in Java reflection.
            // We don't want that because it breaks all sorts of invariants on names which cannot be empty in Kotlin.
            // So we extract the simple name from the full JVM binary name, e.g. "org.test.Foo$1" -> "Foo$1".
            Name.identifier(klass.name.substringAfterLast("."))
        } else {
            Name.identifier(klass.simpleName)
        }

    override val typeParameters: List<ReflectJavaTypeParameter>
        get() = klass.typeParameters.map(::ReflectJavaTypeParameter)

    override val isInterface: Boolean
        get() = klass.isInterface
    override val isAnnotationType: Boolean
        get() = klass.isAnnotation
    override val isEnum: Boolean
        get() = klass.isEnum

    override val isRecord: Boolean
        get() = Java16SealedRecordLoader.loadIsRecord(klass) ?: false

    override val recordComponents: Collection<JavaRecordComponent>
        get() = (Java16SealedRecordLoader.loadGetRecordComponents(klass) ?: emptyArray()).map(::ReflectJavaRecordComponent)

    override val isSealed: Boolean
        get() = Java16SealedRecordLoader.loadIsSealed(klass) ?: false

    override val permittedTypes: Sequence<JavaClassifierType>
        get() = Java16SealedRecordLoader.loadGetPermittedSubclasses(klass)
            ?.map(::ReflectJavaClassifierType)
            ?.asSequence()
            ?: emptySequence()

    override fun equals(other: Any?) = other is ReflectJavaClass && klass == other.klass

    override fun hashCode() = klass.hashCode()

    override fun toString() = this::class.java.name + ": " + klass
}

private object Java16SealedRecordLoader {
    class Cache(
        val isSealed: Method?,
        val getPermittedSubclasses: Method?,
        val isRecord: Method?,
        val getRecordComponents: Method?
    )

    private var _cache: Cache? = null

    private fun buildCache(): Cache {
        val clazz = Class::class.java

        return try {
            Cache(
                clazz.getMethod("isSealed"),
                clazz.getMethod("getPermittedSubclasses"),
                clazz.getMethod("isRecord"),
                clazz.getMethod("getRecordComponents")
            )
        } catch (e: NoSuchMethodException) {
            Cache(null, null, null, null)
        }
    }

    private fun initCache(): Cache {
        var cache = this._cache
        if (cache == null) {
            cache = buildCache()
            this._cache = cache
        }
        return cache
    }

    fun loadIsSealed(clazz: Class<*>): Boolean? {
        val cache = initCache()
        val isSealed = cache.isSealed ?: return null
        return isSealed.invoke(clazz) as Boolean
    }

    fun loadGetPermittedSubclasses(clazz: Class<*>): Array<Class<*>>? {
        val cache = initCache()
        val getPermittedSubclasses = cache.getPermittedSubclasses ?: return null
        @Suppress("UNCHECKED_CAST")
        return getPermittedSubclasses.invoke(clazz) as Array<Class<*>>
    }

    fun loadIsRecord(clazz: Class<*>): Boolean? {
        val cache = initCache()
        val isRecord = cache.isRecord ?: return null
        return isRecord.invoke(clazz) as Boolean
    }

    fun loadGetRecordComponents(clazz: Class<*>): Array<Any>? {
        val cache = initCache()
        val getRecordComponents = cache.getRecordComponents ?: return null
        @Suppress("UNCHECKED_CAST")
        return getRecordComponents.invoke(clazz) as Array<Any>?
    }
}
