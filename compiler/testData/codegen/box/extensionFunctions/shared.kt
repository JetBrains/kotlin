// FILE: lib.kt
inline fun assert(message : String, condition : () -> Boolean) {
    if (!condition())
        throw AssertionError(message)
}

// FILE: main.kt
infix fun <T> T.mustBe(t : T) {
    assert("$this must be $t") {this == t}
}

fun box() : String {
    "lala" mustBe "lala"
    return "OK"
}
