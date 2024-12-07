// RUN_PIPELINE_TILL: BACKEND
data class StringPair(val first: String, val second: String)

infix fun String.to(second: String) = StringPair(this, second)

fun f(a: String?) {
    if (a != null) {
        val b: StringPair = a to a
    }
}