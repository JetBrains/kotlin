// KT-14961
// IGNORE_BACKEND: JVM, JS_IR, WASM
// WITH_RUNTIME

fun listOfFactor(number: Int): List<Int> {
    tailrec fun listOfFactor(number: Int, acc: List<Int>): List<Int> {
        (2..number).forEach {
            if (number % it == 0) return listOfFactor(number / it, acc + it)
        }
        return acc
    }
    return listOfFactor(number, emptyList())
}

fun box(): String {
    val factors = listOfFactor(60)
    return if (factors.size == 4) "OK" else "Fail: $factors"
}
