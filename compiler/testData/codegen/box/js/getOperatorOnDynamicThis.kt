// TARGET_BACKEND: JS
// ISSUE: KT-57988
// FIR_DUMP

inline fun <T : Any> jso(): T = js("({})")
inline fun <T : Any> jso(block: T.() -> Unit): T = jso<T>().apply(block)

external interface Z {
    var a: dynamic
}

fun foo() {
    jso<Z>().apply {
        a = jso {
            this[foo.bar]
        }
    }
}

fun box() = "OK"