private fun privateMethod() = "OK"

internal inline val internalInlineVal: () -> String
    get() = { privateMethod() }

fun box(): String {
    return internalInlineVal.invoke()
}
