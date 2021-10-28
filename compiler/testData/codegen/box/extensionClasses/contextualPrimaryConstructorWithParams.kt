// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

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