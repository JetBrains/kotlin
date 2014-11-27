// !DIAGNOSTICS: -UNUSED_VARIABLE

inline fun<T> foo(block: () -> T):T = block()

fun baz() {
    val x: String = foo {
        val task: String? = null
        if (task == null) {
            return
        } else <!DEBUG_INFO_SMARTCAST!>task<!>
    }
}