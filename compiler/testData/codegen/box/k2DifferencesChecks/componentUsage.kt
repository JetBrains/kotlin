// ORIGINAL: /compiler/testData/diagnostics/tests/deprecated/componentUsage.fir.kt
// WITH_STDLIB
// !DIAGNOSTICS: -UNUSED_VARIABLE

class Data {
    @Deprecated("text")
    operator fun component1(): String = throw Exception()
    operator fun component2(): String = throw Exception()
}

fun use() {
    val (x, y) = Data()
}


fun box() = "OK"
