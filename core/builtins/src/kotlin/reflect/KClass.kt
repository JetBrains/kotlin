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

package kotlin.reflect

/**
 * Represents a class and provides introspection capabilities.
 * Instances of this class are obtainable by the `::class` syntax.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/reflection.html#class-references)
 * for more information.
 *
 * @param T the type of the class.
 */
public interface KClass<T : Any> : KDeclarationContainer, KAnnotatedElement, KClassifier {
    /**
     * The simple name of the class as it was declared in the source code,
     * or `null` if the class has no name (if, for example, it is an anonymous object literal).
     */
    public val simpleName: String?

    /**
     * The fully qualified dot-separated name of the class,
     * or `null` if the class is local or it is an anonymous object literal.
     */
    public val qualifiedName: String?

    /**
     * All functions and properties accessible in this class, including those declared in this class
     * and all of its superclasses. Does not include constructors.
     */
    override val members: Collection<KCallable<*>>

    /**
     * All constructors declared in this class.
     */
    public val constructors: Collection<KFunction<T>>

    /**
     * All classes declared inside this class. This includes both inner and static nested classes.
     */
    public val nestedClasses: Collection<KClass<*>>

    /**
     * The instance of the object declaration, or `null` if this class is not an object declaration.
     */
    public val objectInstance: T?

    /**
     * Returns `true` if [value] is an instance of this class on a given platform.
     */
    public fun isInstance(value: Any?): Boolean

    /**
     * The list of type parameters of this class. This list does *not* include type parameters of outer classes.
     */
    public val typeParameters: List<KTypeParameter>

    /**
     * The list of immediate supertypes of this class, in the order they are listed in the source code.
     */
    public val supertypes: List<KType>

    /**
     * `true` if this class is `final`.
     */
    public val isFinal: Boolean

    /**
     * `true` if this class is `open`.
     */
    public val isOpen: Boolean

    /**
     * `true` if this class is `abstract`.
     */
    public val isAbstract: Boolean

    /**
     * `true` if this class is `sealed`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/classes.html#sealed-classes)
     * for more information.
     */
    public val isSealed: Boolean

    /**
     * `true` if this class is a data class.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/data-classes.html)
     * for more information.
     */
    public val isData: Boolean

    /**
     * `true` if this class is an inner class.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/nested-classes.html#inner-classes)
     * for more information.
     */
    public val isInner: Boolean

    /**
     * `true` if this class is a companion object.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/object-declarations.html#companion-objects)
     * for more information.
     */
    public val isCompanion: Boolean

    /**
     * Returns `true` if [other] is a [KClass] instance representing the same class on a given platform.
     *
     * On JVM this means that the given instance is backed by the same [Class] object as this one. In particular, it requires
     * that the two classes are loaded with the same class loader and have the same name. Note that there are cases where the behavior
     * of this method may seem unintuitive:
     * * For each JVM primitive type, there are two classes at runtime: one for the primitive itself, and another for the wrapper class.
     *   [KClass] instances for those classes are different: [KClass] for `int` is **not equal** to [KClass] for `java.lang.Integer`,
     *   although both have the same qualified name [kotlin.Int].
     * * For JVM arrays of different types, [KClass] instances are different,
     *   although all of them have the same qualified name [kotlin.Array].
     */
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}
