// IS_APPLICABLE: false

inline fun <T, R> T.let(block: (T) -> R): R = block(this)

fun foo(arg: Any?): Any? {
    return arg?.let {
        <caret>x -> x.toString().let { x }
    }
}

