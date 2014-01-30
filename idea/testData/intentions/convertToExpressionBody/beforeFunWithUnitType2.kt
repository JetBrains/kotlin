// "class org.jetbrains.jet.plugin.intentions.ConvertToExpressionBodyAction" "false"

fun foo() {
    <caret>bar()
}

fun bar(): String = "abc"