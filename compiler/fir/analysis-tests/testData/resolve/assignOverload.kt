var result: String = "Fail"

operator fun Any.assign(other: String) {
    result = other
}

fun box(): String {
    val x = 10
    x = "OK"
    return result
}