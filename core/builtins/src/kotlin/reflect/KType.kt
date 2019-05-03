/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Represents a type. Type is usually either a class with optional type arguments,
 * or a type parameter of some declaration, plus nullability.
 */
public interface KType : KAnnotatedElement {
    /**
     * The declaration of the classifier used in this type.
     * For example, in the type `List<String>` the classifier would be the [KClass] instance for [List].
     *
     * Returns `null` if this type is not denotable in Kotlin, for example if it is an intersection type.
     */
    @SinceKotlin("1.1")
    public val classifier: KClassifier?

    /**
     * Type arguments passed for the parameters of the classifier in this type.
     * For example, in the type `Array<out Number>` the only type argument is `out Number`.
     *
     * In case this type is based on an inner class, the returned list contains the type arguments provided for the innermost class first,
     * then its outer class, and so on.
     * For example, in the type `Outer<A, B>.Inner<C, D>` the returned list is `[C, D, A, B]`.
     */
    @SinceKotlin("1.1")
    public val arguments: List<KTypeProjection>

    /**
     * `true` if this type was marked nullable in the source code.
     *
     * For Kotlin types, it means that `null` value is allowed to be represented by this type.
     * In practice it means that the type was declared with a question mark at the end.
     * For non-Kotlin types, it means the type or the symbol which was declared with this type
     * is annotated with a runtime-retained nullability annotation such as [javax.annotation.Nullable].
     *
     * Note that even if [isMarkedNullable] is false, values of the type can still be `null`.
     * This may happen if it is a type of the type parameter with a nullable upper bound:
     *
     * ```
     * fun <T> foo(t: T) {
     *     // isMarkedNullable == false for t's type, but t can be null here when T = "Any?"
     * }
     * ```
     */
    public val isMarkedNullable: Boolean
}

/**
 * Represents a type projection. Type projection is usually the argument to another type in a type usage.
 * For example, in the type `Array<out Number>`, `out Number` is the covariant projection of the type represented by the class `Number`.
 *
 * Type projection is either the star projection, or an entity consisting of a specific type plus optional variance.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#type-projections)
 * for more information.
 */
@SinceKotlin("1.1")
public data class KTypeProjection constructor(
        /**
         * The use-site variance specified in the projection, or `null` if this is a star projection.
         */
        public val variance: KVariance?,
        /**
         * The type specified in the projection, or `null` if this is a star projection.
         */
        public val type: KType?
) {
    public companion object {
        /**
         * Star projection, denoted by the `*` character.
         * For example, in the type `KClass<*>`, `*` is the star projection.
         * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/generics.html#star-projections)
         * for more information.
         */
        public val STAR: KTypeProjection = KTypeProjection(null, null)

        /**
         * Creates an invariant projection of a given type. Invariant projection is just the type itself,
         * without any use-site variance modifiers applied to it.
         * For example, in the type `Set<String>`, `String` is an invariant projection of the type represented by the class `String`.
         */
        public fun invariant(type: KType): KTypeProjection =
                KTypeProjection(KVariance.INVARIANT, type)

        /**
         * Creates a contravariant projection of a given type, denoted by the `in` modifier applied to a type.
         * For example, in the type `MutableList<in Number>`, `in Number` is a contravariant projection of the type of class `Number`.
         */
        public fun contravariant(type: KType): KTypeProjection =
                KTypeProjection(KVariance.IN, type)

        /**
         * Creates a covariant projection of a given type, denoted by the `out` modifier applied to a type.
         * For example, in the type `Array<out Number>`, `out Number` is a covariant projection of the type of class `Number`.
         */
        public fun covariant(type: KType): KTypeProjection =
                KTypeProjection(KVariance.OUT, type)
    }
}
