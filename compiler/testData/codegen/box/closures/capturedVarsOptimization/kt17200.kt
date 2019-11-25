// IGNORE_BACKEND_FIR: JVM_IR
inline fun inlineCall(action: () -> Unit) {
    action()
}

fun test() {
    var width = 1
    inlineCall {
        width += width
    }
}

fun test2() {
    var width = 1L
    val newValue = 1;
    val newValue2 = "123";
    val newValue3 = 2.0;
    inlineCall {
        width += width
    }
}

fun box(): String {
    test()
    test2()
    return "OK"
}