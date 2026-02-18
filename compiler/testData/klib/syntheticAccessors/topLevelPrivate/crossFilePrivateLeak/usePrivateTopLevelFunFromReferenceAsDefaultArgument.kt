// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// NO_CHECK_LAMBDA_INLINING
// FILE: a.kt
private fun funOK() = "OK"

internal inline fun internalInlineFun(ok: () -> String = ::funOK) = ok()

// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
