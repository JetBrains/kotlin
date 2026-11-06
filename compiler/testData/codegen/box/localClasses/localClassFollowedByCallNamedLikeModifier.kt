// ISSUE: KT-90782

var log = ""

fun inline(s: String): String {
    log += "inline($s);"
    return s
}

fun value(f: () -> Unit) {
    log += "value;"
    f()
}

fun private(s: String, f: () -> Unit = {}) {
    log += "private($s);"
    f()
}

fun throwAfterLocalClass(): Int {
    class C
    error("boom")
}

fun localClass(): String =
    try {
        throwAfterLocalClass()
        "no exception"
    } catch (e: IllegalStateException) {
        e.message!!
    }

fun localClassFollowedByCall(): String {
    class C
    inline("a")
    class D<T>
    value {}
    abstract class E
    private("b") { log += "lambda;" }
    data class F(val x: Int)
    private("c")
    return log
}

fun localClassWithConstructorOnNextLine(): String {
    class C
    private constructor(val x: String) {
        constructor() : this("OK")
    }
    return C().x
}

fun box(): String {
    if (localClass() != "boom") return "FAIL 1: ${localClass()}"
    val log = localClassFollowedByCall()
    if (log != "inline(a);value;private(b);lambda;private(c);") return "FAIL 2: $log"
    return localClassWithConstructorOnNextLine()
}
