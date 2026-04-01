// TARGET_BACKEND: WASM
// ^^ JS backend doesn't throw null pointer exception on null from JS side for non-null type

inline fun checkNPE(body: () -> Unit) {
    var throwed = false
    try {
        body()
    } catch (e: NullPointerException) {
        throwed = true
    }
    assertTrue(throwed)
}

external interface EI

fun getJsNullAsNonNullable(): EI =
    js("null")

fun getJsUndefinedAsNonNullable(): EI =
    js("undefined")

fun box(): String {
    checkNPE(::getJsNullAsNonNullable)
    checkNPE(::getJsUndefinedAsNonNullable)

    return "OK"
}
