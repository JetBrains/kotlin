val Int.test = "test"

fun box(): String {
    val x = "a ${1.test}"
    return if (x == "a test") "OK" else "Fail $x"
}
