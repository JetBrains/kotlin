// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"
// ERROR: The integer literal does not conform to the expected type String
// ERROR: Assigning single elements to varargs in named form is forbidden

@Deprecated("", ReplaceWith("newFun()", imports = 123))
fun oldFun() {
    newFun()
}

fun newFun(){}

fun foo() {
    <caret>oldFun()
}
