fun foo() {}

fun box(): String {
    when ("A") {
        "B" -> foo()
        else -> null
    }

    foo()

    return "OK"
}
