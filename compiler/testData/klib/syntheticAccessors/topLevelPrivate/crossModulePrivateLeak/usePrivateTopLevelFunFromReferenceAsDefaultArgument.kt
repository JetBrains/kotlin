// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: a.kt
private fun funOK() = "OK"

internal inline fun internalInlineFun(ok: () -> String = ::funOK) = ok()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
