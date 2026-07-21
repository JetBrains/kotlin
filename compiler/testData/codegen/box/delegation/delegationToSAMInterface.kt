// ISSUE: KT-87545
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:1.9,2.0,2.1,2.2,2.3

fun interface F {
    fun f()
}

object F1 : F by { }

fun box(): String {
    F1.f()
    return "OK"
}
