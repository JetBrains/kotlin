// WITH_STDLIB
// TARGET_BACKEND: JVM
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
        set(select(Out("OK"), makeOut())) // problem is here
        // we keep set in PARTIALLY analyzed state, while we're also trying to incorporate it both into return-type constraining
        // and builder inference, which introduces non-trivial loop of codependency between those two steps
        // Due to that, we leak type variables from set call-graph to the system of buildBox, while those variables are already
        // fixed
    }

fun <S> select(a: S, b: S): S = a

fun <M> makeOut(): Out<M>? = null

fun box(): String {
    if (foo().boxed!!.v != "OK") return "FAIL"
    return "OK"
}
