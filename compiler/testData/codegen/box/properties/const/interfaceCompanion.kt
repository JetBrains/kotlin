// TARGET_BACKEND: JVM

// WITH_STDLIB

interface KInt {

    companion object {
        const val a = "a"
        const val b = "b$a"
    }
}

fun box(): String {
    val a = KInt::class.java.getField("a").get(null)
    val b = KInt::class.java.getField("b").get(null)

    if (a !== KInt.a) return "fail 1: KInt.a !== KInt.Companion.a"
    if (b !== KInt.b) return "fail 2: KInt.b !== KInt.Companion.b"
    if (b !== "ba") return "fail 2: 'ba' !== KInt.Companion.b"

    return "OK"
}
