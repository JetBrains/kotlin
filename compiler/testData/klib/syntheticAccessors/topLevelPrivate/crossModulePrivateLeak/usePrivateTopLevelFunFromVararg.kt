// MODULE: lib
// FILE: a.kt
private fun funOK() = "OK"

internal inline fun internalInlineFunWithVararg(vararg strings: String) = strings[2]

internal inline fun internalInlineFun() = internalInlineFunWithVararg(funOK(), "NOK", funOK())

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
