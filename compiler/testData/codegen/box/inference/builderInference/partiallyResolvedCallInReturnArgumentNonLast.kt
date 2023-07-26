// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-58742

class Out<out V>(val v: V)
class Box<R> {
    var boxed: R? = null

    fun set(newValue: R) {
        boxed = newValue
    }
}

fun <R> buildBox(fill: Box<R>.() -> Unit): Box<R> {
    return Box<R>().also {
        it.fill()
    }
}

fun foo() =
    buildBox {
        if (true) return@buildBox set(select(Out("OK"), makeOut())) // problem is here
        Unit
    }

fun <S> select(a: S, b: S): S = a

fun <M> makeOut(): Out<M>? = null

fun box(): String {
    if (foo().boxed!!.v != "OK") return "FAIL"
    return "OK"
}
