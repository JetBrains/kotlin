// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-58741

fun foo() =
    buildBox {
        set(select(Out("OK"), makeOut()))
    }


class Out<out V>(val v: V)
class Box<R> {
    var boxed: R? = null

    fun set(newValue: R): String {
        boxed = newValue
        return "set"
    }
}

fun <R> buildBox(fill: Box<R>.() -> String): Box<R> {
    return Box<R>().also {
        require(it.fill() == "set")
    }
}

fun <S> select(a: S, b: S): S = a

fun <M> makeOut(): Out<M>? = null


fun box(): String {
    if (foo().boxed!!.v != "OK") return "FAIL"
    return "OK"
}