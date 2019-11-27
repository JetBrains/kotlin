// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var result = ""
    fun add(s: String) {
        result += s
    }
    add("O")
    add("K")
    return result
}