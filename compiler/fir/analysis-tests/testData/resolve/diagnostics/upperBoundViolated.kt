interface A

class B<T> where T : A
class C : A
typealias GGG = C
typealias HHH = GGG
typealias JJJ = B<C>

fun <T : A> fest() {

}

fun test() {
    val b1 = B<<!UPPER_BOUND_VIOLATED!>Int<!>>()
    val b2 = B<C>()
    val b3 = B<<!UPPER_BOUND_VIOLATED!>Any?<!>>()
    val b4 = B<<!UNRESOLVED_REFERENCE!>UnexistingType<!>>()<!UNRESOLVED_REFERENCE!>NL<!><!SYNTAX!><<!>Int<!SYNTAX!>><!>()NumberPhile<!SYNTAX!><!>
    val b5 = B<<!UPPER_BOUND_VIOLATED!>B<<!UNRESOLVED_REFERENCE!>UnexistingType<!>><!>>()
    fest<<!UPPER_BOUND_VIOLATED!>Boolean<!>>()
    fest<C>()
    fest<HHH>()
    fest<<!UPPER_BOUND_VIOLATED!>JJJ<!>>()
}

open class S<F, G : F>
class T<U, Y : U> : S<U, Y>()

fun <K, L : K> rest() {
    val o1 = S<K, L>()
    val o2 = S<K, K>()
    val o3 = S<L, L>()

    val o4 = S<S<K, L>, T<K, L>>()
    val o5 = S<S<K, L>, <!UPPER_BOUND_VIOLATED!>T<K, K><!>>()
    val o5 = S<S<L, L>, <!UPPER_BOUND_VIOLATED!>T<K, L><!>>()

    val o6 = S<Any, <!UPPER_BOUND_VIOLATED!>T<S<K, L>, String><!>>()
    val o7 = S<Any, T<S<K, L>, Nothing>>()
}

class NumColl<T : Collection<Number>>
typealias NL<K> = NumColl<List<K>>
val test7 = NL<Int>()<!UNRESOLVED_REFERENCE!>NumberPhile<!><!SYNTAX!><!>
val test8 = NL<String>()

class NumberPhile<T: Number>(x: T)
val np1 = NumberPhile(10)
val np2 = NumberPhile(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)
