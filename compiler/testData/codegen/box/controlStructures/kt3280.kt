// IGNORE_BACKEND_FIR: JVM_IR
fun foo() {
    var x = 0
    do {
        x++
        var y = x + 5
    } while (y < 10)
    if (x != 5) throw AssertionError("$x")
}

fun bar() {
    var b = false
    do {
        var x = "X"
        var y = "Y"
        b = true
    } while (x + y != "XY")
    if (!b) throw AssertionError()
}

fun box(): String {
    foo()
    bar()
    return "OK"
}
