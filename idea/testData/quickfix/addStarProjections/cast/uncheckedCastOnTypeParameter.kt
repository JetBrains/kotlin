// "class org.jetbrains.kotlin.idea.quickfix.ChangeToStarProjectionFix" "false"
// ACTION: Convert to expression body
fun <T> get(column: String, map: Map<String, Any>): T {
    return map[column] as <caret>T
}
