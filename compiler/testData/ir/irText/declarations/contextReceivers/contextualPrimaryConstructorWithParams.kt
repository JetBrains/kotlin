// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class O(val o: String)

context(O)
class OK(val k: String) {
    val result: String = o + k
}

fun box(): String {
    return with(O("O")) {
        val ok = OK("K")
        ok.result
    }
}
