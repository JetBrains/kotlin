// WITH_STDLIB
// ISSUE: KT-70625
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: NATIVE:1.9,2.0
// ^^^ KT-70625 Fixed in 2.1.0-Beta1
// DUMP_IR

fun <T> mutate(x: MutableList<T>): MutableList<T> {
    return x
}

fun box(): String {
    val x : MutableList<*> = mutableListOf(1)
    x.also(::mutate)

    return "OK"
}