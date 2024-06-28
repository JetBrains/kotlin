// FIR_IDENTICAL
// ISSUE: KT-66243

class A<T>
class Test<T> {
    fun add(a: T) {}
    var lambdaInVariable: ((A<T>) -> Unit)? = null
}

fun <T> builder(x: Test<T>.() -> Unit): Test<T> {
    return Test<T>().apply(x)
}

fun check() {
    val x = builder {
        add(1)
        lambdaInVariable =  {}
    }
}
