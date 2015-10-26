fun <<!UPPER_BOUND_CANNOT_BE_ARRAY!>A : Array<Any><!>> f1() {}
fun <T, <!UPPER_BOUND_CANNOT_BE_ARRAY!>A : Array<out T><!>> f2() {}
fun <S, T : S, <!INCONSISTENT_TYPE_PARAMETER_BOUNDS, UPPER_BOUND_CANNOT_BE_ARRAY!>A<!>> f3() where A : Array<out S>, A : Array<out T> {}

fun <<!UPPER_BOUND_CANNOT_BE_ARRAY!>T : <!FINAL_UPPER_BOUND!>IntArray<!><!>> f4() {}

fun <<!UPPER_BOUND_CANNOT_BE_ARRAY!>T<!>> f5() where T : Array<Any> {}

val <<!UPPER_BOUND_CANNOT_BE_ARRAY!>T : Array<Any?><!>> T.v: String get() = ""

class C2<T, <!UPPER_BOUND_CANNOT_BE_ARRAY!>A : Array<out T><!>>
interface C3<S, T : S, <!INCONSISTENT_TYPE_PARAMETER_BOUNDS, UPPER_BOUND_CANNOT_BE_ARRAY!>A<!>> where A : Array<out S>, A : Array<out T>

fun foo() {
    class C1<<!UPPER_BOUND_CANNOT_BE_ARRAY!>A : Array<Any><!>> {
        fun <<!UPPER_BOUND_CANNOT_BE_ARRAY!>A : Array<Any><!>, <!UPPER_BOUND_CANNOT_BE_ARRAY!>B : Array<Any><!>, C : A> f() {}
    }
}
