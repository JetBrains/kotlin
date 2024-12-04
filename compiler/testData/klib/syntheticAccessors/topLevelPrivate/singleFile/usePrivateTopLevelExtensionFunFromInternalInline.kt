private fun String.privateFun() = "${this}K"

internal inline fun internalInlineFun() = "O".privateFun()

fun box(): String {
    return internalInlineFun()
}
