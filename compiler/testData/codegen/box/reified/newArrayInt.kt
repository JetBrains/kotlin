// WITH_RUNTIME

inline fun<reified T> createArray(n: Int, crossinline block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

fun box(): String {

    val x = createArray<Int>(5) { 3 }

    assert(x.all { it == 3 })
    return "OK"
}
