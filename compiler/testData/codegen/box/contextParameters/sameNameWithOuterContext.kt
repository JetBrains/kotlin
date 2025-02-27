// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

context(a: String)
val p
    get() = context(a: String) fun (): String { return a }

fun box(): String {
    with("not OK") {
        return p("OK")
    }
}