data class StringPair(val first: String, val second: String)

infix fun String.to(second: String) = StringPair(this, second)

fun hashMapOf(pair: StringPair): MutableMap<String, String> {
}

fun F() : MutableMap<String, String> {
    val value: String? = "xyz"
    if (value == null) throw Error()
    // Smart cast should be here
    return hashMapOf("sss" to value)  
}