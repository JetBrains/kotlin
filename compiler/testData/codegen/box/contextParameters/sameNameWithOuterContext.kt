// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

context(a: String)
val p
    get() = context(a: String) fun (): String { return a }

fun box(): String {
    with("not OK") {
        return p("OK")
    }
}
