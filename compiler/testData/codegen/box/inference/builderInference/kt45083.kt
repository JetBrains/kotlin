// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM

import kotlin.experimental.ExperimentalTypeInference
object Hello {
    val hello = "hello"
}

@OptIn(ExperimentalTypeInference::class)
fun <E> buildList0(@BuilderInference builder: MutableList<E>.() -> Unit): List<E> = mutableListOf<E>().apply { builder() }

val numbers = buildList0 {
    add(Hello.let { it::hello }.get())
}

fun box(): String {
    numbers
    return "OK"
}
