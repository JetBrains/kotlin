// FIR_IDENTICAL
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
interface Either<out A, out B>
interface Left<out A>: Either<Bar<A>, Nothing> {
    val value: A
}
interface Right<out B>: Either<Nothing, B> {
    val value: B
}

interface Foo<out K>
interface Bar<out K>

class C1(val v1: Int)
class C2(val v2: Int)

fun _is_l(e: Either<Foo<C1>, C2>): Any {
    if (e is <!NO_TYPE_ARGUMENTS_ON_RHS!>Left<!>) {
        return e.value.<!UNRESOLVED_REFERENCE!>v1<!>
    }
    return e
}

fun _is_r(e: Either<C1, C2>): Any {
    if (e is Right) {
        return e.value.v2
    }
    return e
}