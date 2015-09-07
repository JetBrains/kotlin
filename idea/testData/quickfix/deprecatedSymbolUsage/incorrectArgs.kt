// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"
// ERROR: An integer literal does not conform to the expected type kotlin.String

@Deprecated("", ReplaceWith("newFun()", imports = 123))
fun oldFun() {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun()
}
