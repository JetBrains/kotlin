// "class org.jetbrains.jet.plugin.intentions.ConvertToExpressionBodyAction" "false"

fun foo(): String {
    val v = 1
    <caret>return "abc"
}