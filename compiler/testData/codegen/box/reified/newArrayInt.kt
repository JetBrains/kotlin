// WITH_STDLIB

inline fun<reified T> createArray(n: Int, crossinline block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

fun box(): String {

    val x = createArray<Int>(5) { 3 }

    require(x.all { it == 3 })
    return "OK"
}
