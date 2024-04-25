// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// FIR status: context receivers aren't yet supported

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
