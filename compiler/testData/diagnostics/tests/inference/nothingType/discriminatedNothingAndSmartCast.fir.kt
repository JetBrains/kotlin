// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

interface ExpectedType

inline fun <reified M> parse(): M? = TODO()

fun test(s: String?, silent: Boolean) {
    val result: ExpectedType =
        if (s != null) {
            parse() ?: TODO()
        } else if (silent) {
            return
        } else {
            throw Exception()
        }

    s.length
}
