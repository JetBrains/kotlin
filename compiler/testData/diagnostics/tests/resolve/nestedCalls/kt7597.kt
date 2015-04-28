// !CHECK_TYPE

trait Inv<I>

fun <S, T: S> Inv<T>.reduce2(): S = null!!

fun test(a: Inv<Int>): Int {
    val b = 1 <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> a.reduce2()
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
}