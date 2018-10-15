// LANGUAGE_VERSION: 1.2

fun box(): String {
    lateinit var ok: String
    run {
        ok = "OK"
    }
    return ok
}