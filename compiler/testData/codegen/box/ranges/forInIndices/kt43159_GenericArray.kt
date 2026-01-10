// WITH_STDLIB
// FILE: lib.kt

class Value<T>(val value: T) {
    inline fun <R> runBlock(block: (T) -> R) = block(value)
}

// FILE: main.kt

fun <T> Value<Array<T>>.test() =
    runBlock {
        var sum = 0
        for (i in it.indices)
            sum = sum * 10 + i
        sum
    }

fun box(): String {
    if (Value<Array<Int>>(Array<Int>(4) { 0 }).test() != 123) return "fail"
    return "OK"
}