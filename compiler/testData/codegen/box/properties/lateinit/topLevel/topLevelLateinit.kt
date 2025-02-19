// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

lateinit var ok: String

fun box(): String {
    run {
        ok = "OK"
    }
    return ok
}
