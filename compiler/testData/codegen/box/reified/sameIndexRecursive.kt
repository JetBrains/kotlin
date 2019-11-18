// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

inline fun<reified T1, reified T2> createArray(n: Int, crossinline block: () -> Pair<T1, T2>): Pair<Array<T1>, Array<T2>> {
    return Pair(Array(n) { block().first }, Array(n) { block().second })
}

inline fun<T1, T2, T3, T4, T5, T6, reified R> recursive(
        crossinline block: () -> R
): Pair<Array<R>, Array<R>> {
    return createArray(5) { Pair(block(), block()) }
}

fun box(): String {
    val y = createArray(5) { Pair(1, "test") }
    val x = recursive<Int, Int, Int, Int, Int, Int, String>(){ "abc" }

    assert(y.first.all { it == 1 } )
    assert(y.second.all { it == "test" })
    assert(x.first.all { it == "abc" })
    assert(x.second.all { it == "abc" })
    return "OK"
}
