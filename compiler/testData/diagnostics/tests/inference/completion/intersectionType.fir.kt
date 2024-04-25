// DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: D1, b: D2) {
    id<Out<Base>>(
        makeOut(
            select(a, b)
        )
    )
    id<Inv<Base>>(
        makeInv(
            select(a, b)
        )
    )
    id<In<Base>>(
        makeIn(
            select(a, b)
        )
    )
    id<In<In<Base>>>(
        makeInIn(
            select(a, b)
        )
    )
}

interface Base
interface B1 : Base
interface B2 : Base
interface D1 : B1, B2
interface D2 : B1, B2

fun <S> select(a: S, b: S): S = TODO()
class Inv<T>
class Out<out O>
class In<in I>

fun <K> id(arg: K) = arg
fun <T> makeInv(arg: T): Inv<T> = TODO()
fun <T> makeOut(arg: T): Out<T> = TODO()
fun <T> makeIn(arg: T): In<T> = TODO()
fun <T> makeInIn(arg: T): In<In<T>> = TODO()
