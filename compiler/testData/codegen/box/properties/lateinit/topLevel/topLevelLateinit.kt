// IGNORE_BACKEND: JS_IR
// LANGUAGE_VERSION: 1.2

lateinit var ok: String

fun box(): String {
    run {
        ok = "OK"
    }
    return ok
}