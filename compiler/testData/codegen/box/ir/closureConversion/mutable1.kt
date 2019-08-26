// IGNORE_BACKEND: WASM
fun box(): String {
    var result = ""
    fun add(s: String) {
        result += s
    }
    add("O")
    add("K")
    return result
}