// WITH_STDLIB

fun box(): String {
    return when(val foo = 42UL) {
        42UL -> "OK"
        else -> "Fail"
    }
}
