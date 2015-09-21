// !DIAGNOSTICS: -UNUSED_VARIABLE

class Data {
    @Deprecated("text")
    operator fun component1(): String = throw Exception()
    operator fun component2(): String = throw Exception()
}

fun use() {
    val (<!DEPRECATED_SYMBOL_WITH_MESSAGE!>x<!>, y) = Data()
}