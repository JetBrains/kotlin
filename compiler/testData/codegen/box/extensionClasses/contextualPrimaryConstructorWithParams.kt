// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

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
