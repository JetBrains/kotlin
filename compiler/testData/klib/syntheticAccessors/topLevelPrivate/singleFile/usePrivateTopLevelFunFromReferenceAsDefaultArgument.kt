// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// NO_CHECK_LAMBDA_INLINING
private fun funOK() = "OK"

internal inline fun internalInlineFun(ok: () -> String = ::funOK) = ok()

fun box(): String {
    return internalInlineFun()
}
