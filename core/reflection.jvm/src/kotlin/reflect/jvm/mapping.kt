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

package kotlin.reflect.jvm

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.Intrinsic
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.*

// Kotlin reflection -> Java reflection

/**
 * Returns a Java [Class] instance corresponding to the given [KClass] instance.
 */
@Intrinsic("kotlin.KClass.java.property")
public val <T> KClass<T>.java: Class<T>
    get() = (this as KClassImpl<T>).jClass

/**
 * Returns a Java [Class] instance that represents a Kotlin package.
 * The methods and fields of this class are generated from top level functions and properties in the Kotlin package.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#package-level-functions)
 * for more information.
 */
public val KPackage.javaFacade: Class<*>
    get() = (this as KPackageImpl).jClass


/**
 * Returns a Java [Field] instance corresponding to the backing field of the given property,
 * or `null` if the property has no backing field.
 */
public val KProperty<*>.javaField: Field?
    get() = (this as KPropertyImpl<*>).javaField

/**
 * Returns a Java [Method] instance corresponding to the getter of the given property,
 * or `null` if the property has no getter, for example in case of a simple private `val` in a class.
 */
public val KProperty<*>.javaGetter: Method?
    get() = getter.javaMethod

/**
 * Returns a Java [Method] instance corresponding to the setter of the given mutable property,
 * or `null` if the property has no setter, for example in case of a simple private `var` in a class.
 */
public val KMutableProperty<*>.javaSetter: Method?
    get() = setter.javaMethod


/**
 * Returns a Java [Method] instance corresponding to the given Kotlin function,
 * or `null` if this function is a constructor or cannot be represented by a Java [Method].
 */
public val KFunction<*>.javaMethod: Method?
    get() = when (this) {
        is KCallableImpl<*> -> (caller as? FunctionCaller.Method)?.method
        else -> null
    }

/**
 * Returns a Java [Constructor] instance corresponding to the given Kotlin function,
 * or `null` if this function is not a constructor or cannot be represented by a Java [Constructor].
 */
@suppress("UNCHECKED_CAST")
public val <T> KFunction<T>.javaConstructor: Constructor<T>?
    get() = when (this) {
        is KFunctionImpl -> (caller as? FunctionCaller.Constructor)?.constructor as? Constructor<T>
        else -> null
    }



// Java reflection -> Kotlin reflection

// TODO: getstatic $kotlinClass or go to foreignKClasses
/**
 * Returns a [KClass] instance corresponding to the given Java [Class] instance.
 */
public val <T> Class<T>.kotlin: KClass<T>
    get() = KClassImpl(this)

/**
 * Returns a [KPackage] instance corresponding to the Java [Class] instance.
 * The given class is generated from top level functions and properties in the Kotlin package.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#package-level-functions)
 * for more information.
 */
public val Class<*>.kotlinPackage: KPackage?
    get() = if (getSimpleName().endsWith("Package") &&
                getAnnotation(javaClass<kotlin.jvm.internal.KotlinPackage>()) != null) KPackageImpl(this) else null


/**
 * Returns a [KProperty] instance corresponding to the given Java [Field] instance,
 * or `null` if this field cannot be represented by a Kotlin property
 * (for example, if it is a synthetic field).
 */
public val Field.kotlinProperty: KProperty<*>?
    get() {
        if (isSynthetic()) return null

        // TODO: fields in package parts
        // TODO: optimize (search by name)
        return getDeclaringClass().kotlin.memberProperties.firstOrNull { it.javaField == this }
    }


/**
 * Returns a [KFunction] instance corresponding to the given Java [Method] instance,
 * or `null` if this method cannot be represented by a Kotlin function
 * (for example, if it is a synthetic method).
 */
public val Method.kotlinFunction: KFunction<*>?
    get() {
        if (isSynthetic()) return null

        if (Modifier.isStatic(getModifiers())) {
            getDeclaringClass().kotlinPackage?.let { pkg ->
                return pkg.functions.firstOrNull { it.javaMethod == this }
            }
        }

        return getDeclaringClass().kotlin.functions.firstOrNull { it.javaMethod == this }
    }

/**
 * Returns a [KFunction] instance corresponding to the given Java [Constructor] instance,
 * or `null` if this constructor cannot be represented by a Kotlin function
 * (for example, if it is a synthetic constructor).
 */
public val <T> Constructor<T>.kotlinFunction: KFunction<T>?
    get() {
        if (isSynthetic()) return null

        return getDeclaringClass().kotlin.constructors.firstOrNull { it.javaConstructor == this }
    }
