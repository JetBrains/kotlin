// FILE: a.kt
private fun foo(x: String) = x

private inline fun privateInlineFunO(o: String = "O") = foo(o)

internal inline fun internalInlineFunK(k: String = "K") = foo(k)

internal inline fun internalInlineFun(o: String = privateInlineFunO(), k: String = internalInlineFunK()) = o + k

// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
