fun foo(): String {
    return if (true) {
        var x = "OK"
        fun foo() { x += "fail" }
        x
    } else "fail"
}

fun box(): String {
    return foo()
}
