// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

class Foo<A>

fun <K> bar(x: Foo<K>): Unit {}

fun <E> foo(block: (Foo<E>) -> Unit): E = null as E

interface FlowCollector<T> {
    fun emit(value: T)
}

@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
fun <I> flow(@BuilderInference block: FlowCollector<I>.() -> Unit): I = null as I

fun adapt(): Unit = flow {
    emit(foo { coroutine -> bar(coroutine) })
}

fun box(): String {
    adapt()
    return "OK"
}