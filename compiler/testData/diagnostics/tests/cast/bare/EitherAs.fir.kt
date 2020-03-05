// !CHECK_TYPE

interface Either<out A, out B>
interface Left<out A>: Either<A, Nothing>
interface Right<out B>: Either<Nothing, B>

class C1(val v1: Int)
class C2(val v2: Int)

fun _as_left(e: Either<C1, C2>): Any {
    val v = e as Left
    return checkSubtype<Left<C1>>(v)
}

fun _as_right(e: Either<C1, C2>): Any {
    val v = e as Right
    return <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><Right<C2>>(v)
}