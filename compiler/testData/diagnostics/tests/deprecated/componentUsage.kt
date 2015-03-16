// !DIAGNOSTICS: -UNUSED_VARIABLE

class Data {
    deprecated("text")
    fun component1(): String = throw Exception()
    fun component2(): String = throw Exception()
}

fun use() {
    val (<!DEPRECATED_SYMBOL_WITH_MESSAGE!>x<!>, y) = Data()
}