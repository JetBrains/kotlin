// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

object Host {
    class StringDelegate(val s: String) {
        operator fun getValue(receiver: String, p: Any) = receiver + s
    }

    operator fun String.provideDelegate(host: Any?, p: Any) = StringDelegate(this)

    val String.plusK by "K"

    val ok = "O".plusK
}

