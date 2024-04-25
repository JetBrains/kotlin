// DIAGNOSTICS: -UNUSED_PARAMETER

fun test(a: D1, b: D2) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Base>")!>id<Out<Base>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<{B1 & B2}>")!>makeOut(
            <!DEBUG_INFO_EXPRESSION_TYPE("{B1 & B2}")!>select(a, b)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Base>")!>id<Inv<Base>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Base>")!>makeInv(
            <!DEBUG_INFO_EXPRESSION_TYPE("{B1 & B2}")!>select(a, b)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<Base>")!>id<In<Base>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<Base>")!>makeIn(
            <!DEBUG_INFO_EXPRESSION_TYPE("{B1 & B2}")!>select(a, b)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<In<Base>>")!>id<In<In<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<In<{B1 & B2}>>")!>makeInIn(
            <!DEBUG_INFO_EXPRESSION_TYPE("{B1 & B2}")!>select(a, b)<!>
        )<!>
    )<!>
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
