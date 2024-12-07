// FIR_IDENTICAL
fun foo() {
    var i = 1
    while (i < 10) {
        bar()
        i = i.inc()
    }
}

fun bar() {}
