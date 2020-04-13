// FLOW: OUT

fun foo(<caret>p: String) {
    val v1 = p.let { value -> bar(value) }

    val v2 = p.let { it }

    val v3 = p.let {
        val it = "a"
        it
    }

    val v4 = p.let(::zoo)
}

fun bar(s: String) = s
fun zoo(s: String) = s

inline fun <T, R> T.let(block: (T) -> R): R {
    return block(this)
}
