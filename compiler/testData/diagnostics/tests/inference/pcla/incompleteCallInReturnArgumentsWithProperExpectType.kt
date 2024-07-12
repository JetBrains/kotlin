// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

fun test1() =
    buildBoxUnit {
        set(select(Out(""), makeOut()))
        mat() // Should be able to infer Unit here
    }

fun test2() =
    buildBoxProperType {
        set(select(Out(""), makeOut()))
        mat() // Should be able to infer String here
    }


class Out<out V>(val v: V)
class Box<R> {
    var boxed: R? = null

    fun set(newValue: R) {
        boxed = newValue
    }
}

fun <R> buildBoxUnit(fill: Box<R>.() -> Unit): Box<R> {
    return Box<R>().also {
        it.fill()
    }
}

fun <R> buildBoxProperType(fill: Box<R>.() -> String): Box<R> {
    return Box<R>().also {
        it.fill()
    }
}

fun <S> select(a: S, b: S): S = a

fun <M> makeOut(): Out<M>? = null

fun <M> mat(): M = null!!
