private fun foo(): String = "b"

fun box(): String =
    if (foo() == "a") {
        "FAIL1"
    } else if (foo() == "b") {
        "OK"
    } else {
        "FAIL2"
    }
