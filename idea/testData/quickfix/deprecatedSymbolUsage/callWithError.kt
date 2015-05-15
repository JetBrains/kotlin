// "class org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageFix" "false"
// ERROR: Too many arguments for kotlin.deprecated internal fun oldFun(): kotlin.Unit defined in root package

@deprecated("", ReplaceWith("newFun()"))
fun oldFun() {
}

fun newFun(){}

fun foo() {
    <caret>oldFun(123)
}
