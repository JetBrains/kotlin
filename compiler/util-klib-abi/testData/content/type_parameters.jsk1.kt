// !LANGUAGE: +ContextReceivers
// MODULE: type_parameters_library

// About this test:
// This is an adapted version of the original `type_parameters.kt` test data file specially for JS IR K1 compiler.
// For details see the comments below in this file.

package type_parameters.test

interface Interface<A, B, C>
class TypeParameterInSuperTypes<A, B, C> : Interface<List<List<C>>, Map<B, A>, Triple<C, B, A>>

fun <A : CharSequence, B : A, C : B> interDependentTypeParameters(p1: A, p2: B, p3: C) = Unit
fun <A> multipleBounds(p1: A) where A : CharSequence, A : Appendable, A : Number = Unit

inline fun <reified R, T> functionWithReifiedParameter(p1: R, p2: T) = Unit

// Note: The JS K1 compiler considers getters and setters of these two properties to be conflicting with each other:
//
//   var <P : Number> P.property: P get() = TODO()
//       set(_) = Unit
//   var <P : Number> P?.property: P get() = TODO()
//       set(_) = Unit
//
// To overcome this we just use different upper bounds:
//   var <P : List<*>> P.property: P get() = TODO()
//       set(_) = Unit
//   var <P : Number> P?.property: P get() = TODO()
//       set(_) = Unit
var <P : List<*>> P.property: P get() = TODO()
    set(_) = Unit
var <P : Number> P?.property: P get() = TODO()
    set(_) = Unit

// Note: The JS K1 compiler considers the following two declarations conflicting with each other:
//
//   fun <F : Number> one(p1: F)  = Unit
//   fun <F : Number> one(p1: F?) = Unit
//
// Therefore, instead of testing the following three declarations altogether:
//
//   fun <F : Number>  one(p1: F)  = Unit // <-- these two are conflicting with each other
//   fun <F : Number>  one(p1: F?) = Unit // <-- these two are conflicting with each other
//   fun <F : Number?> one(p1: F)  = Unit
//
// we should split them into two pairs:
//
//   fun <F : Number>   one(p1: F)  = Unit
//   fun <F : Number?>  one(p1: F)  = Unit
//   fun <F : List<*>>  one(p1: F?) = Unit
//   fun <F : List<*>?> one(p1: F)  = Unit
fun <F : Number> one(p1: F) = Unit
fun <F : Number?> one(p1: F) = Unit
fun <F : List<*>> one(p1: F?) = Unit
fun <F : List<*>?> one(p1: F) = Unit

class Outer<O : Appendable>(p1: O) {
    inner class TypeParameterInSuperTypes<A, B> : Interface<List<List<O>>, Map<B, A>, Triple<O, B, A>>

    var O.property: O get() = TODO()
        set(_) = Unit
    var <P : List<*>> P.property: P get() = TODO()
        set(_) = Unit
    var <P : Number> P?.property: P get() = TODO()
        set(_) = Unit

    fun one(p1: O) = Unit
    fun <F : Number> one(p1: F) = Unit
    fun <F : Number> two(p0: F, p1: O) = Unit
    fun <F : Number> two(p0: O, p1: F) = Unit

    class Nested<N : CharSequence>(p1: N) {
        inner class TypeParameterInSuperTypes<A, B> : Interface<List<List<N>>, Map<B, A>, Triple<N, B, A>>

        var N.property: N get() = TODO()
            set(_) = kotlin.Unit
        var <P : List<*>> P.property: P get() = TODO()
            set(_) = Unit
        var <P : Number> P?.property: P get() = TODO()
            set(_) = Unit

        fun one(p1: N) = Unit
        fun <F : Number> one(p1: F) = Unit
        fun <F : Number> two(p0: F, p1: N) = Unit
        fun <F : Number> two(p0: N, p1: F) = Unit

        inner class Inner<I : StringBuilder>(p1: N, p2: I) {
            inner class TypeParameterInSuperTypes<A> : Interface<List<List<N>>, Map<I, A>, Triple<N, I, A>>

            var N.property: N get() = TODO()
                set(_) = kotlin.Unit
            var I.property: I get() = TODO()
                set(_) = kotlin.Unit
            var <P : List<*>> P.property: P get() = TODO()
                set(_) = Unit
            var <P : Number> P?.property: P get() = TODO()
                set(_) = Unit

            fun one(p1: N) = Unit
            fun one(p1: I) = Unit
            fun <F : Number> one(p1: F) = Unit
            fun two(p0: N, p1: I) = Unit
            fun two(p0: I, p1: N) = Unit
            fun <F : Number> two(p0: N, p1: I) = Unit
            fun <F : Number> two(p0: N, p1: F) = Unit
            fun <F : Number> two(p0: I, p1: N) = Unit
            fun <F : Number> two(p0: I, p1: F) = Unit
            fun <F : Number> two(p0: F, p1: N) = Unit
            fun <F : Number> two(p0: F, p1: I) = Unit
            fun <F : Number> three(p1: N, p2: I, p3: F) = Unit
            fun <F : Number> three(p1: N, p2: F, p3: I) = Unit
            fun <F : Number> three(p1: I, p2: N, p3: F) = Unit
            fun <F : Number> three(p1: I, p2: F, p3: N) = Unit
            fun <F : Number> three(p1: F, p2: N, p3: I) = Unit
            fun <F : Number> three(p1: F, p2: I, p3: N) = Unit
        }
    }

    inner class Inner<I : CharSequence>(p1: O, p2: I) {
        inner class TypeParameterInSuperTypes<A> : Interface<List<List<O>>, Map<I, A>, Triple<O, I, A>>

        var O.property: O get() = TODO()
            set(_) = kotlin.Unit
        var I.property: I get() = TODO()
            set(_) = kotlin.Unit
        var <P : List<*>> P.property: P get() = TODO()
            set(_) = Unit
        var <P : Number> P?.property: P get() = TODO()
            set(_) = Unit

        fun one(p1: O) = Unit
        fun one(p1: I) = Unit
        fun <F : Number> one(p1: F) = Unit
        fun two(p0: O, p1: I) = Unit
        fun two(p0: I, p1: O) = Unit
        fun <F : Number> two(p0: O, p1: I) = Unit
        fun <F : Number> two(p0: O, p1: F) = Unit
        fun <F : Number> two(p0: I, p1: O) = Unit
        fun <F : Number> two(p0: I, p1: F) = Unit
        fun <F : Number> two(p0: F, p1: O) = Unit
        fun <F : Number> two(p0: F, p1: I) = Unit
        fun <F : Number> three(p1: O, p2: I, p3: F) = Unit
        fun <F : Number> three(p1: O, p2: F, p3: I) = Unit
        fun <F : Number> three(p1: I, p2: O, p3: F) = Unit
        fun <F : Number> three(p1: I, p2: F, p3: O) = Unit
        fun <F : Number> three(p1: F, p2: O, p3: I) = Unit
        fun <F : Number> three(p1: F, p2: I, p3: O) = Unit

        inner class Inner2<I2 : StringBuilder>(p1: O, p2: I, p3: I2) {
            inner class TypeParameterInSuperTypes : Interface<List<List<O>>, Map<I, I2>, Triple<O, I, I2>>

            var I2.property: I2 get() = TODO()
                set(_) = kotlin.Unit

            fun <F : Number> four(p1: O, p2: I, p3: I2, p4: F) = Unit
        }
    }
}
