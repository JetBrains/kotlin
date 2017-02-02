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

@file:JvmName("KClasses")
@file:Suppress("UNCHECKED_CAST", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package kotlin.reflect

import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.cast
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.declaredMemberExtensionProperties
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.defaultType
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberExtensionFunctions
import kotlin.reflect.full.memberExtensionProperties
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.safeCast
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.full.staticProperties
import kotlin.reflect.full.superclasses

/**
 * Returns the primary constructor of this class, or `null` if this class has no primary constructor.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/classes.html#constructors)
 * for more information.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'primaryConstructor' from kotlin.reflect.full package", ReplaceWith("this.primaryConstructor", "kotlin.reflect.full.primaryConstructor"), level = DeprecationLevel.WARNING)
inline val <T : Any> KClass<T>.primaryConstructor: KFunction<T>?
    get() = this.primaryConstructor


/**
 * Returns a [KClass] instance representing the companion object of a given class,
 * or `null` if the class doesn't have a companion object.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'companionObject' from kotlin.reflect.full package", ReplaceWith("this.companionObject", "kotlin.reflect.full.companionObject"), level = DeprecationLevel.WARNING)
inline val KClass<*>.companionObject: KClass<*>?
    get() = this.companionObject

/**
 * Returns an instance of the companion object of a given class,
 * or `null` if the class doesn't have a companion object.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'companionObjectInstance' from kotlin.reflect.full package", ReplaceWith("this.companionObjectInstance", "kotlin.reflect.full.companionObjectInstance"), level = DeprecationLevel.WARNING)
inline val KClass<*>.companionObjectInstance: Any?
    get() = this.companionObjectInstance


/**
 * Returns a type corresponding to the given class with type parameters of that class substituted as the corresponding arguments.
 * For example, for class `MyMap<K, V>` [defaultType] would return the type `MyMap<K, V>`.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'defaultType' from kotlin.reflect.full package", ReplaceWith("this.defaultType", "kotlin.reflect.full.defaultType"), level = DeprecationLevel.WARNING)
inline val KClass<*>.defaultType: KType
    get() = this.defaultType


/**
 * Returns all functions and properties declared in this class.
 * Does not include members declared in supertypes.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'declaredMembers' from kotlin.reflect.full package", ReplaceWith("this.declaredMembers", "kotlin.reflect.full.declaredMembers"), level = DeprecationLevel.ERROR)
inline val KClass<*>.declaredMembers: Collection<KCallable<*>>
    get() = this.declaredMembers

/**
 * Returns all functions declared in this class, including all non-static methods declared in the class
 * and the superclasses, as well as static methods declared in the class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'functions' from kotlin.reflect.full package", ReplaceWith("this.functions", "kotlin.reflect.full.functions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.functions: Collection<KFunction<*>>
    get() = this.functions

/**
 * Returns static functions declared in this class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'staticFunctions' from kotlin.reflect.full package", ReplaceWith("this.staticFunctions", "kotlin.reflect.full.staticFunctions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.staticFunctions: Collection<KFunction<*>>
    get() = this.staticFunctions

/**
 * Returns non-extension non-static functions declared in this class and all of its superclasses.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'memberFunctions' from kotlin.reflect.full package", ReplaceWith("this.memberFunctions", "kotlin.reflect.full.memberFunctions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.memberFunctions: Collection<KFunction<*>>
    get() = this.memberFunctions

/**
 * Returns extension functions declared in this class and all of its superclasses.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'memberExtensionFunctions' from kotlin.reflect.full package", ReplaceWith("this.memberExtensionFunctions", "kotlin.reflect.full.memberExtensionFunctions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.memberExtensionFunctions: Collection<KFunction<*>>
    get() = this.memberExtensionFunctions

/**
 * Returns all functions declared in this class.
 * If this is a Java class, it includes all non-static methods (both extensions and non-extensions)
 * declared in the class and the superclasses, as well as static methods declared in the class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'declaredFunctions' from kotlin.reflect.full package", ReplaceWith("this.declaredFunctions", "kotlin.reflect.full.declaredFunctions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.declaredFunctions: Collection<KFunction<*>>
    get() = this.declaredFunctions

/**
 * Returns non-extension non-static functions declared in this class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'declaredMemberFunctions' from kotlin.reflect.full package", ReplaceWith("this.declaredMemberFunctions", "kotlin.reflect.full.declaredMemberFunctions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.declaredMemberFunctions: Collection<KFunction<*>>
    get() = this.declaredMemberFunctions

/**
 * Returns extension functions declared in this class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'declaredMemberExtensionFunctions' from kotlin.reflect.full package", ReplaceWith("this.declaredMemberExtensionFunctions", "kotlin.reflect.full.declaredMemberExtensionFunctions"), level = DeprecationLevel.WARNING)
inline val KClass<*>.declaredMemberExtensionFunctions: Collection<KFunction<*>>
    get() = this.declaredMemberExtensionFunctions

/**
 * Returns static properties declared in this class.
 * Only properties representing static fields of Java classes are considered static.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'staticProperties' from kotlin.reflect.full package", ReplaceWith("this.staticProperties", "kotlin.reflect.full.staticProperties"), level = DeprecationLevel.WARNING)
inline val KClass<*>.staticProperties: Collection<KProperty0<*>>
    get() = this.staticProperties

/**
 * Returns non-extension properties declared in this class and all of its superclasses.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'memberProperties' from kotlin.reflect.full package", ReplaceWith("this.memberProperties", "kotlin.reflect.full.memberProperties"), level = DeprecationLevel.WARNING)
inline val <T : Any> KClass<T>.memberProperties: Collection<KProperty1<T, *>>
    get() = this.memberProperties

/**
 * Returns extension properties declared in this class and all of its superclasses.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'memberExtensionProperties' from kotlin.reflect.full package", ReplaceWith("this.memberExtensionProperties", "kotlin.reflect.full.memberExtensionProperties"), level = DeprecationLevel.WARNING)
inline val <T : Any> KClass<T>.memberExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = this.memberExtensionProperties

/**
 * Returns non-extension properties declared in this class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'declaredMemberProperties' from kotlin.reflect.full package", ReplaceWith("this.declaredMemberProperties", "kotlin.reflect.full.declaredMemberProperties"), level = DeprecationLevel.WARNING)
inline val <T : Any> KClass<T>.declaredMemberProperties: Collection<KProperty1<T, *>>
    get() = this.declaredMemberProperties

/**
 * Returns extension properties declared in this class.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'declaredMemberExtensionProperties' from kotlin.reflect.full package", ReplaceWith("this.declaredMemberExtensionProperties", "kotlin.reflect.full.declaredMemberExtensionProperties"), level = DeprecationLevel.WARNING)
inline val <T : Any> KClass<T>.declaredMemberExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = this.declaredMemberExtensionProperties


/**
 * Immediate superclasses of this class, in the order they are listed in the source code.
 * Includes superclasses and superinterfaces of the class, but does not include the class itself.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'superclasses' from kotlin.reflect.full package", ReplaceWith("this.superclasses", "kotlin.reflect.full.superclasses"), level = DeprecationLevel.ERROR)
inline val KClass<*>.superclasses: List<KClass<*>>
    get() = this.superclasses

/**
 * All supertypes of this class, including indirect ones, in no particular order.
 * There is not more than one type in the returned collection that has any given classifier.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'allSupertypes' from kotlin.reflect.full package", ReplaceWith("this.allSupertypes", "kotlin.reflect.full.allSupertypes"), level = DeprecationLevel.ERROR)
inline val KClass<*>.allSupertypes: Collection<KType>
    get() = this.allSupertypes
/**
 * All superclasses of this class, including indirect ones, in no particular order.
 * Includes superclasses and superinterfaces of the class, but does not include the class itself.
 * The returned collection does not contain more than one instance of any given class.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'allSuperclasses' from kotlin.reflect.full package", ReplaceWith("this.allSuperclasses", "kotlin.reflect.full.allSuperclasses"), level = DeprecationLevel.ERROR)
inline val KClass<*>.allSuperclasses: Collection<KClass<*>>
    get() = this.allSuperclasses
/**
 * Returns `true` if `this` class is the same or is a (possibly indirect) subclass of [base], `false` otherwise.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'isSubclassOf' from kotlin.reflect.full package", ReplaceWith("this.isSubclassOf(base)", "kotlin.reflect.full.isSubclassOf"), level = DeprecationLevel.ERROR)
inline fun KClass<*>.isSubclassOf(base: KClass<*>): Boolean =
    this.isSubclassOf(base)

/**
 * Returns `true` if `this` class is the same or is a (possibly indirect) superclass of [derived], `false` otherwise.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'isSuperclassOf' from kotlin.reflect.full package", ReplaceWith("this.isSuperclassOf(derived)", "kotlin.reflect.full.isSuperclassOf"), level = DeprecationLevel.ERROR)
inline fun KClass<*>.isSuperclassOf(derived: KClass<*>): Boolean =
        this.isSuperclassOf(derived)


/**
 * Casts the given [value] to the class represented by this [KClass] object.
 * Throws an exception if the value is `null` or if it is not an instance of this class.
 *
 * @see [KClass.isInstance]
 * @see [KClass.safeCast]
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'cast' from kotlin.reflect.full package", ReplaceWith("this.cast(value)", "kotlin.reflect.full.cast"), level = DeprecationLevel.ERROR)
inline fun <T : Any> KClass<T>.cast(value: Any?): T {
    return this.cast(value)
}

/**
 * Casts the given [value] to the class represented by this [KClass] object.
 * Returns `null` if the value is `null` or if it is not an instance of this class.
 *
 * @see [KClass.isInstance]
 * @see [KClass.cast]
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'safeCast' from kotlin.reflect.full package", ReplaceWith("this.safeCast(value)", "kotlin.reflect.full.safeCast"), level = DeprecationLevel.ERROR)
inline fun <T : Any> KClass<T>.safeCast(value: Any?): T? {
    return this.safeCast(value)
}


/**
 * Creates a new instance of the class, calling a constructor which either has no parameters or all parameters of which are optional
 * (see [KParameter.isOptional]). If there are no or many such constructors, an exception is thrown.
 */
@SinceKotlin("1.1")
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'createInstance' from kotlin.reflect.full package", ReplaceWith("this.createInstance()", "kotlin.reflect.full.createInstance"), level = DeprecationLevel.ERROR)
inline fun <T : Any> KClass<T>.createInstance(): T {
    return this.createInstance()
}
