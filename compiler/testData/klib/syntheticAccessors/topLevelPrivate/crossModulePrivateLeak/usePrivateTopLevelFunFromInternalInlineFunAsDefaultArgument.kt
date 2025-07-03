// MODULE: lib
// FILE: a.kt
private fun funOK(ok: String) = ok
internal inline fun internalInlineFunOK(ok: String = "OK") = funOK(ok)

internal inline fun internalInlineFun(ok: String = internalInlineFunOK()) = ok

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
