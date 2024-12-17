// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

interface Out1<out T1> {
    fun get1(): T1
}

interface Out2<out T2> {
    fun get2(): T2
}

fun <K, V : Out1<Out1<K>>> foo(x: K, tt: (V) -> Unit): V = TODO()

fun bar(x: Out2<Out2<String>>) {
    // NB: The resulting constraint on Vv has the typeDepth (4) more than any of the input type (2)
    val q: Any = foo(x) { it -> it.get1().get1().get2().get2().length }
}
