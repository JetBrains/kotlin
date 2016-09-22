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
@file:Suppress("UNCHECKED_CAST")

package kotlin.reflect

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.DFS
import kotlin.reflect.jvm.internal.KCallableImpl
import kotlin.reflect.jvm.internal.KClassImpl
import kotlin.reflect.jvm.internal.KFunctionImpl
import kotlin.reflect.jvm.internal.KTypeImpl

/**
 * Returns the primary constructor of this class, or `null` if this class has no primary constructor.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/classes.html#constructors)
 * for more information.
 */
val <T : Any> KClass<T>.primaryConstructor: KFunction<T>?
    get() = (this as KClassImpl<T>).constructors.firstOrNull {
        ((it as KFunctionImpl).descriptor as ConstructorDescriptor).isPrimary
    }


/**
 * Returns a [KClass] instance representing the companion object of a given class,
 * or `null` if the class doesn't have a companion object.
 */
val KClass<*>.companionObject: KClass<*>?
    get() = nestedClasses.firstOrNull {
        (it as KClassImpl<*>).descriptor.isCompanionObject
    }

/**
 * Returns an instance of the companion object of a given class,
 * or `null` if the class doesn't have a companion object.
 */
val KClass<*>.companionObjectInstance: Any?
    get() = companionObject?.objectInstance


/**
 * Returns a type corresponding to the given class with type parameters of that class substituted as the corresponding arguments.
 * For example, for class `MyMap<K, V>` [defaultType] would return the type `MyMap<K, V>`.
 */
@Deprecated("This function creates a type which rarely makes sense for generic classes. " +
            "For example, such type can only be used in signatures of members of that class. " +
            "Use starProjectedType or createType() for clearer semantics.")
val KClass<*>.defaultType: KType
    get() = KTypeImpl((this as KClassImpl<*>).descriptor.defaultType) { jClass }


/**
 * Returns all functions and properties declared in this class.
 * Does not include members declared in supertypes.
 */
val KClass<*>.declaredMembers: Collection<KCallable<*>>
    get() = (this as KClassImpl).data().declaredMembers

/**
 * Returns all functions declared in this class, including all non-static methods declared in the class
 * and the superclasses, as well as static methods declared in the class.
 */
val KClass<*>.functions: Collection<KFunction<*>>
    get() = members.filterIsInstance<KFunction<*>>()

/**
 * Returns static functions declared in this class.
 */
val KClass<*>.staticFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl).data().allStaticMembers.filterIsInstance<KFunction<*>>()

/**
 * Returns non-extension non-static functions declared in this class and all of its superclasses.
 */
val KClass<*>.memberFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl).data().allNonStaticMembers.filter { it.isNotExtension && it is KFunction<*> } as Collection<KFunction<*>>

/**
 * Returns extension functions declared in this class and all of its superclasses.
 */
val KClass<*>.memberExtensionFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl).data().allNonStaticMembers.filter { it.isExtension && it is KFunction<*> } as Collection<KFunction<*>>

/**
 * Returns all functions declared in this class.
 * If this is a Java class, it includes all non-static methods (both extensions and non-extensions)
 * declared in the class and the superclasses, as well as static methods declared in the class.
 */
val KClass<*>.declaredFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl).data().declaredMembers.filterIsInstance<KFunction<*>>()

/**
 * Returns non-extension non-static functions declared in this class.
 */
val KClass<*>.declaredMemberFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl).data().declaredNonStaticMembers.filter { it.isNotExtension && it is KFunction<*> } as Collection<KFunction<*>>

/**
 * Returns extension functions declared in this class.
 */
val KClass<*>.declaredMemberExtensionFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl).data().declaredNonStaticMembers.filter { it.isExtension && it is KFunction<*> } as Collection<KFunction<*>>

/**
 * Returns static properties declared in this class.
 * Only properties representing static fields of Java classes are considered static.
 */
val KClass<*>.staticProperties: Collection<KProperty0<*>>
    get() = (this as KClassImpl).data().allStaticMembers.filter { it.isNotExtension && it is KProperty0<*> } as Collection<KProperty0<*>>

/**
 * Returns non-extension properties declared in this class and all of its superclasses.
 */
val <T : Any> KClass<T>.memberProperties: Collection<KProperty1<T, *>>
    get() = (this as KClassImpl<T>).data().allNonStaticMembers.filter { it.isNotExtension && it is KProperty1<*, *> } as Collection<KProperty1<T, *>>

/**
 * Returns extension properties declared in this class and all of its superclasses.
 */
val <T : Any> KClass<T>.memberExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = (this as KClassImpl<T>).data().allNonStaticMembers.filter { it.isExtension && it is KProperty2<*, *, *> } as Collection<KProperty2<T, *, *>>

/**
 * Returns non-extension properties declared in this class.
 */
val <T : Any> KClass<T>.declaredMemberProperties: Collection<KProperty1<T, *>>
    get() = (this as KClassImpl<T>).data().declaredNonStaticMembers.filter { it.isNotExtension && it is KProperty1<*, *> } as Collection<KProperty1<T, *>>

/**
 * Returns extension properties declared in this class.
 */
val <T : Any> KClass<T>.declaredMemberExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = (this as KClassImpl<T>).data().declaredNonStaticMembers.filter { it.isExtension && it is KProperty2<*, *, *> } as Collection<KProperty2<T, *, *>>


private val KCallableImpl<*>.isExtension: Boolean
    get() = descriptor.extensionReceiverParameter != null

private val KCallableImpl<*>.isNotExtension: Boolean
    get() = !isExtension

/**
 * Immediate superclasses of this class, in the order they are listed in the source code.
 * Includes superclasses and superinterfaces of the class, but does not include the class itself.
 */
val KClass<*>.superclasses: List<KClass<*>>
    get() = supertypes.mapNotNull { it.classifier as? KClass<*> }

/**
 * All supertypes of this class, including indirect ones, in no particular order.
 * There is not more than one type in the returned collection that has any given classifier.
 */
val KClass<*>.allSupertypes: Collection<KType>
    get() = DFS.dfs(
            supertypes,
            DFS.Neighbors { current ->
                val klass = current.classifier as? KClass<*> ?: throw KotlinReflectionInternalError("Supertype not a class: $current")
                val supertypes = klass.supertypes
                val typeArguments = current.arguments
                if (typeArguments.isEmpty()) supertypes
                else TypeSubstitutor.create((current as KTypeImpl).type).let { substitutor ->
                    supertypes.map { supertype ->
                        val substituted = substitutor.substitute((supertype as KTypeImpl).type, Variance.INVARIANT)
                                          ?: throw KotlinReflectionInternalError("Type substitution failed: $supertype ($current)")
                        KTypeImpl(substituted) {
                            // TODO
                            TODO("Java type for supertype")
                        }
                    }
                }
            },
            DFS.VisitedWithSet(),
            object : DFS.NodeHandlerWithListResult<KType, KType>() {
                override fun beforeChildren(current: KType): Boolean {
                    result.add(current)
                    return true
                }
            }
    )

/**
 * All superclasses of this class, including indirect ones, in no particular order.
 * Includes superclasses and superinterfaces of the class, but does not include the class itself.
 * The returned collection does not contain more than one instance of any given class.
 */
val KClass<*>.allSuperclasses: Collection<KClass<*>>
    get() = allSupertypes.map { supertype ->
        supertype.classifier as? KClass<*> ?: throw KotlinReflectionInternalError("Supertype not a class: $supertype")
    }

/**
 * Returns `true` if `this` class is the same or is a (possibly indirect) subclass of [base], `false` otherwise.
 */
fun KClass<*>.isSubclassOf(base: KClass<*>): Boolean =
        this == base ||
        DFS.ifAny(listOf(this), KClass<*>::superclasses) { it == base }

/**
 * Returns `true` if `this` class is the same or is a (possibly indirect) superclass of [derived], `false` otherwise.
 */
fun KClass<*>.isSuperclassOf(derived: KClass<*>): Boolean =
        derived.isSubclassOf(this)


/**
 * Casts the given [value] to the class represented by this [KClass] object.
 * Throws an exception if the value is `null` or if it is not an instance of this class.
 *
 * @see [KClass.isInstance]
 * @see [KClass.safeCast]
 */
fun <T : Any> KClass<T>.cast(value: Any?): T {
    if (!isInstance(value)) throw TypeCastException("Value cannot be cast to $qualifiedName")
    return value as T
}

/**
 * Casts the given [value] to the class represented by this [KClass] object.
 * Returns `null` if the value is `null` or if it is not an instance of this class.
 *
 * @see [KClass.isInstance]
 * @see [KClass.cast]
 */
fun <T : Any> KClass<T>.safeCast(value: Any?): T? {
    return if (isInstance(value)) value as T else null
}


/**
 * Creates a new instance of the class, calling a constructor which either has no parameters or all parameters of which are optional
 * (see [KParameter.isOptional]). If there are no or many such constructors, an exception is thrown.
 */
fun <T : Any> KClass<T>.createInstance(): T {
    // TODO: throw a meaningful exception
    val noArgsConstructor = constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
                            ?: throw IllegalArgumentException("Class should have a single no-arg constructor: $this")

    return noArgsConstructor.callBy(emptyMap())
}
