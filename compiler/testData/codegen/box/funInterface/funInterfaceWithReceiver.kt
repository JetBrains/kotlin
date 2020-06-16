// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR_ES6
// SKIP_DCE_DRIVEN

fun interface FunWithReceiver {
    fun String.foo(): String
}

val prop = FunWithReceiver { this }

fun bar(s: String, f: FunWithReceiver): String {
    return with(f) {
        s.foo()
    }
}

fun box(): String {
    val r1 = with(prop) {
        "OK".foo()
    }

    if (r1 != "OK") return "failed 1"

    return bar("O") { this + "K" }
}
