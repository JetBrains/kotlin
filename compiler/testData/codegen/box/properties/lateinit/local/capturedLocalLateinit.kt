// IGNORE_BACKEND: JVM_IR
// LANGUAGE_VERSION: 1.2

fun runNoInline(f: () -> Unit) = f()

fun box(): String {
    lateinit var ok: String
    runNoInline {
        ok = "OK"
    }
    return ok
}