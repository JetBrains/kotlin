// "class org.jetbrains.jet.plugin.intentions.ConvertToExpressionBodyAction" "false"

fun foo(p: Boolean): String {
    if (p) {
        <caret>return "abc"
    }
    else {
        return "def"
    }
}