// IGNORE_BACKEND_K2: ANY
// LANGUAGE: +ContextReceivers, -ContextParameters

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
