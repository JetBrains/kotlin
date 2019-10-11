// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    return when(val foo = 42UL) {
        42UL -> "OK"
        else -> "Fail"
    }
}
