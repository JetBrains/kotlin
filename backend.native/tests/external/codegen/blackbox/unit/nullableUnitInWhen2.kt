fun foo() {}

fun box(): String {
    when ("A") {
        "B" -> null
        else -> foo()
    }

    foo()

    return "OK"
}
