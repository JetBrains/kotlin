private fun o() = "O"
private fun k() = "K"

internal inline fun internalInlineFun(oo: String = o(), kk: () -> String = { k() }) = oo + kk()

fun box(): String {
    return internalInlineFun()
}
