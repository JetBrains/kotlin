// ISSUE: KT-62863
// FIR_IDENTICAL
// WITH_STDLIB
// DUMP_IR

class Some<V : Any>(val map: Map<String, V>) {
    fun test(key: String): String {
        return when (val value = map.getValue(key)) {
            is String -> addK(value)
            else -> "Fail: $value"
        }
    }

    fun addK(s: String): String {
        return s + "K"
    }
}

fun box(): String {
    val x = Some<String>(mapOf("key" to "O"))
    return x.test("key")
}
