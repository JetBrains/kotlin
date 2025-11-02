fun foo(): Unit {}

fun box(): String {
    return when (foo()) {
        Unit -> "OK"
        else -> "Fail"
    }
}