// "class org.jetbrains.jet.plugin.intentions.ConvertToExpressionBodyAction" "false"

var a = 1
var b = 2

fun foo() {
    <caret>a = b
}