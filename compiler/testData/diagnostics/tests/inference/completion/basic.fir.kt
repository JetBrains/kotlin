// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

interface Bound
class Cls : Bound

class Inv<T>(val prop: T)
class In<in I>(param: I)
class InB<in I : Bound>(param: I)
class Out<out O>(val prop: O)

fun <K> id(arg: K): K = arg
fun <W> makeInv(arg: W): Inv<W> = TODO()
fun <O> wrapOut(arg: O): Inv<out O> = TODO()
fun <I> wrapIn(arg: I): Inv<in I> = TODO()

fun test1(cls: Cls) {
    id(
        Inv(cls)
    )
}

fun test2(cls: Cls) {
    id<Inv<Bound>>(
        Inv(cls)
    )
}

fun test3(cls: Cls) {
    id<Out<Bound>>(
        Out(cls)
    )
}

fun test4(cls: Cls) {
    id(
        Out(cls)
    )
}

fun test5(cls: Cls) {
    id(
        In(cls)
    )
}

fun test6(cls: Cls) {
    id<In<Bound>>(
        In(cls)
    )
}

fun test7(cls: Cls) {
    id(
        wrapOut(cls)
    )
}

fun test8(cls: Cls) {
    id(
        wrapIn(cls)
    )
}

fun test9(cls: Cls) {
    id(
        InB(cls)
    )
}
