// "class org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageFix" "false"

@deprecated("", ReplaceWith("="))
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
