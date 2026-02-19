// MODULE: lib
// FILE: a.kt
private fun funO() = "O"

private fun funK() = "K"

private inline fun privateInlineFunWithVararg(vararg strings: String) = strings[0]

internal inline fun internalInlineFunWithVararg(vararg strings: String) = strings[2]

internal inline fun internalInlineFun() =
    privateInlineFunWithVararg(funO(), "NOK", funK()) +
            internalInlineFunWithVararg(funO(), "NOK", funK())

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
