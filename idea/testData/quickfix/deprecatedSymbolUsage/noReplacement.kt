// "class org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageFix" "false"

@deprecated("")
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
