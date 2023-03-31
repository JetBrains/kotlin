// FIR_IDENTICAL
// ISSUE: KT-57665
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> T.foo(a: String, b: (T) -> Unit) {
    this.asDynamic().goo(jso {
        this.asjhasdas
    })
}

fun <T : Any> jso(): T =
    js("({})")

fun <T : Any> jso(
    block: T.() -> Unit,
): T = jso<T>().apply(block)
