private fun foo(): String = "b"

fun box(): String =
    when {
        "a" == foo() -> "FAIL1"
        "b" == foo() -> "OK"
        else -> "FAIL3"
    }
