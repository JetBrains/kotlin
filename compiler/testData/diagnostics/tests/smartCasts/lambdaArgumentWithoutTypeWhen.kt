// See KT-10223

inline fun <T> using(input: Any?, f: (Any?) -> T): T = f(input)

val input: Any? = null

// Error:(32, 24) Type inference failed. Expected type mismatch: inferred type is kotlin.Any? but kotlin.String was expected
val test4: String = using(input) {
    when (it) {
        is String -> <!DEBUG_INFO_SMARTCAST!>it<!>
        else -> throw RuntimeException()
    }
}