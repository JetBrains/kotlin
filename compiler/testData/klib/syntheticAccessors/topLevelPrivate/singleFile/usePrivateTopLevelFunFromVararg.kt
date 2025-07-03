private fun funOK() = "OK"

internal inline fun internalInlineFunWithVararg(vararg strings: String) = strings[2]

internal inline fun internalInlineFun() = internalInlineFunWithVararg(funOK(), "NOK", funOK())

fun box(): String {
    return internalInlineFun()
}
