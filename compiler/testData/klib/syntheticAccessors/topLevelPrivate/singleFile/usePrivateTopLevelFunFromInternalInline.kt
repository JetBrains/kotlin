private fun privateFun() = "OK"

internal inline fun internalInlineFun() = privateFun()

fun box(): String {
    return internalInlineFun()
}
