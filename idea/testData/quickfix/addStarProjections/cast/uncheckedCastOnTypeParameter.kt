// "Change type arguments to <>" "false"
// ACTION: Convert to expression body
fun <T> get(column: String, map: Map<String, Any>): T {
    return map[column] as <caret>T
}
