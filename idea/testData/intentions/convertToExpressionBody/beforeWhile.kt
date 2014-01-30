// "class org.jetbrains.jet.plugin.intentions.ConvertToExpressionBodyAction" "false"

fun foo(p: Boolean): String {
    <caret>while(true) { }
}