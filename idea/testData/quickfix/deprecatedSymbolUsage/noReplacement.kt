// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

@deprecated("")
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
