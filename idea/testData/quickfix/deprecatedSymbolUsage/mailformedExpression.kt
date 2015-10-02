// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

@Deprecated("", ReplaceWith("order.safeAddItem(...)"))
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
