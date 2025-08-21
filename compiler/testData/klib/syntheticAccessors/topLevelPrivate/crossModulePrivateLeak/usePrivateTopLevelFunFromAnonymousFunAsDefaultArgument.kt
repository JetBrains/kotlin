// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: a.kt
private fun funOK() = "OK"

@Suppress("NOT_YET_SUPPORTED_IN_INLINE")
internal inline fun internalInlineFun(ok: () -> String = fun() = funOK()) = ok()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
