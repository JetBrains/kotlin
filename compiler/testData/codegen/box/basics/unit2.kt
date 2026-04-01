fun box(): String {
    val x = foo()
    if (x.toString() != "kotlin.Unit") return "FAIL: $x"
    return "OK"
}

fun foo() {
    return Unit
}
