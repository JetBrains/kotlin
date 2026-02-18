fun foo() = "OK"

fun box(): String =
    when {
        foo() == "" -> "FAIL1"
        foo() == "foo" -> "FAIL2"
        else -> foo()
    }
