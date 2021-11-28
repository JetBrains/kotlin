// WITH_STDLIB
fun foo(): Array<Boolean> {
    return arrayOf(
        0.0 / 0 == 0.0 / 0,
        0.0F > -0.0F,
        0.0.equals(-0.0),
        (0.0 / 0.0).equals(1.0 / 0.0)
    )
}

fun box(): String {
    if (foo().any { it == true })
        return "fail: ${foo().contentDeepToString()}"
    return "OK"
}
