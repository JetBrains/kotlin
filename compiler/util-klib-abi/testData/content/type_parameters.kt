// !LANGUAGE: +ContextReceivers
// MODULE: type_parameters_library

package type_parameters.test

interface Interface<A, B, C>
class TypeParameterInSuperTypes<A, B, C> : Interface<List<List<C>>, Map<B, A>, Triple<C, B, A>>

fun <A : CharSequence, B : A, C : B> interDependentTypeParameters(p1: A, p2: B, p3: C) = Unit
fun <A> multipleBounds(p1: A) where A : CharSequence, A : Appendable, A : Number = Unit

inline fun <reified R, T> functionWithReifiedParameter(p1: R, p2: T) = Unit

fun <T00, T01 : Any, T02 : CharSequence?, T03 : CharSequence, T04 : Appendable?, T05 : Appendable, T06 : Number?, T07 : Number, T08 : List<*>?, T09 : List<*>,
     T10, T11 : Any, T12 : CharSequence?, T13 : CharSequence, T14 : Appendable?, T15 : Appendable, T16 : Number?, T17 : Number, T18 : List<*>?, T19 : List<*>,
     T20, T21 : Any, T22 : CharSequence?, T23 : CharSequence, T24 : Appendable?, T25 : Appendable, T26 : Number?, T27 : Number, T28 : List<*>?, T29 : List<*>,
     T30, T31 : Any, T32 : CharSequence?, T33 : CharSequence, T34 : Appendable?, T35 : Appendable, T36 : Number?, T37 : Number, T38 : List<*>?, T39 : List<*>,
     T40, T41 : Any, T42 : CharSequence?, T43 : CharSequence, T44 : Appendable?, T45 : Appendable, T46 : Number?, T47 : Number, T48 : List<*>?, T49 : List<*>,
     T50, T51 : Any, T52 : CharSequence?, T53 : CharSequence, T54 : Appendable?, T55 : Appendable, T56 : Number?, T57 : Number, T58 : List<*>?, T59 : List<*>> lotsOfTypeParameters(): CharSequence = ""

var <P : Number> P.property: P get() = this
    set(_) = Unit
var <P : CharSequence> P?.property: P? get() = this
    set(_) = Unit
var <P : Appendable?> P.property: P get() = this
    set(_) = Unit
var <P : List<*>?> P?.property: P? get() = this
    set(_) = Unit

fun <F : Number> one(p1: F): F = p1
fun <F : CharSequence> one(p1: F?): F? = p1
fun <F : Appendable?> one(p1: F): F = p1
fun <F : List<*>?> one(p1: F?): F? = p1

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
