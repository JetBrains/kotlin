trait Either<out A, out B>
trait Left<out A>: Either<A, Nothing>
trait Right<out B>: Either<Nothing, B>

class C1(val v1: Int)
class C2(val v2: Int)

fun _as_left(e: Either<C1, C2>): Any? {
    val v = e as? Left
    return v: Left<C1>?
}

fun _as_right(e: Either<C1, C2>): Any? {
    val v = e as? Right
    return v: Right<C2>?
}