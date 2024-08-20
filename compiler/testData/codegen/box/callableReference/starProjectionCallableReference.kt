// WITH_STDLIB
// IGNORE_BACKEND_K2: JVM_IR, NATIVE, WASM
// IGNORE_BACKEND_K1: WASM
// ISSUE: KT-70625
// DUMP_IR

fun <T> mutate(x: MutableList<T>): MutableList<T> {
    return x
}

fun box(): String {
    val x : MutableList<*> = mutableListOf(1)
    x.also(::mutate)

    return "OK"
}