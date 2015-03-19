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

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.Reflection
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.*

// Kotlin reflection -> Java reflection

/**
 * Returns a Java [Class] instance corresponding to the given [KClass] instance.
 */
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
 * Returns a Java [Method] instance corresponding to the getter of the given property,
 * or `null` if the property has no getter, for example in case of a simple private `val` in a class.
 */
public val KProperty<*>.javaGetter: Method?
    get() = (this as? KPropertyImpl<*>)?.getter

/**
 * Returns a Java [Method] instance corresponding to the setter of the given mutable property,
 * or `null` if the property has no setter, for example in case of a simple private `var` in a class.
 */
public val KMutableProperty<*>.javaSetter: Method?
    get() = (this as? KMutablePropertyImpl<*>)?.setter


/**
 * Returns a Java [Field] instance corresponding to the backing field of the given top level property,
 * or `null` if the property has no backing field.
 */
public val KTopLevelVariable<*>.javaField: Field?
    get() = (this as KPropertyImpl<*>).field

/**
 * Returns a Java [Method] instance corresponding to the getter of the given top level property.
 */
public val KTopLevelVariable<*>.javaGetter: Method
    get() = (this as KTopLevelVariableImpl<*>).getter

/**
 * Returns a Java [Method] instance corresponding to the setter of the given top level property.
 */
public val KMutableTopLevelVariable<*>.javaSetter: Method
    get() = (this as KMutableTopLevelVariableImpl<*>).setter


/**
 * Returns a Java [Method] instance corresponding to the getter of the given top level extension property.
 */
public val KTopLevelExtensionProperty<*, *>.javaGetter: Method
    get() = (this as KTopLevelExtensionPropertyImpl<*, *>).getter

/**
 * Returns a Java [Method] instance corresponding to the setter of the given top level extension property.
 */
public val KMutableTopLevelExtensionProperty<*, *>.javaSetter: Method
    get() = (this as KMutableTopLevelExtensionPropertyImpl<*, *>).setter


/**
 * Returns a Java [Field] instance corresponding to the backing field of the given member property,
 * or `null` if the property has no backing field.
 */
public val KMemberProperty<*, *>.javaField: Field?
    get() = (this as KPropertyImpl<*>).field


// Java reflection -> Kotlin reflection

// TODO: getstatic $kotlinClass or go to foreignKClasses
/**
 * Returns a [KClass] instance corresponding to the given Java [Class] instance.
 */
public val <T> Class<T>.kotlin: KClass<T>
    get() = KClassImpl(this)

// TODO: getstatic $kotlinPackage
// TODO: make nullable or throw exception
/**
 * Returns a [KPackage] instance corresponding to the Java [Class] instance.
 * The given class is generated from top level functions and properties in the Kotlin package.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#package-level-functions)
 * for more information.
 */
public val Class<*>.kotlinPackage: KPackage
    get() = KPackageImpl(this)


/**
 * Returns a [KProperty] instance corresponding to the given Java [Field] instance,
 * or `null` if this field cannot be represented by a Kotlin property
 * (for example, if it is a synthetic field).
 */
public val Field.kotlin: KProperty<*>?
    get() {
        if (isSynthetic()) return null

        val clazz = getDeclaringClass()
        val name = getName()
        val modifiers = getModifiers()
        val static = Modifier.isStatic(modifiers)
        val final = Modifier.isFinal(modifiers)
        if (static) {
            val kPackage = clazz.kotlinPackage
            return if (final) Reflection.topLevelVariable(name, kPackage) else Reflection.mutableTopLevelVariable(name, kPackage)
        }
        else {
            val kClass = clazz.kotlin
            return if (final) Reflection.memberProperty(name, kClass) else Reflection.mutableMemberProperty(name, kClass)
        }
    }
