private fun funOK(ok: String) = ok
internal inline fun internalInlineFunOK(ok: String = "OK") = funOK(ok)

internal inline fun internalInlineFun(ok: String = internalInlineFunOK()) = ok

fun box(): String {
    return internalInlineFun()
}
