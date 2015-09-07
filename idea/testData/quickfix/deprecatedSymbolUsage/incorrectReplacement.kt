// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

@deprecated("", ReplaceWith("="))
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
