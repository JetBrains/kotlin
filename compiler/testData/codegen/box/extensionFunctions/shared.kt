// IGNORE_BACKEND_FIR: JVM_IR
infix fun <T> T.mustBe(t : T) {
    assert("$this must be $t") {this == t}
}

inline fun assert(message : String, condition : () -> Boolean) {
    if (!condition())
        throw AssertionError(message)
}

fun box() : String {
    "lala" mustBe "lala"
    return "OK"
}
