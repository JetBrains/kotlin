// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
interface Either<out A, out B>
interface Left<out A>: Either<A, Nothing> {
    val value: A
}
interface Right<out B>: Either<Nothing, B> {
    val value: B
}

class C1(val v1: Int)
class C2(val v2: Int)

fun _is_l(e: Either<C1, C2>): Any {
    if (e !is Left) {
        return e
    }
    return e.value.v1
}

fun _is_r(e: Either<C1, C2>): Any {
    if (e !is Right) {
        return e
    }
    return e.value.<!UNRESOLVED_REFERENCE!>v2<!>
}