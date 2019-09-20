// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"
// ERROR: Too many arguments for public fun oldFun(): Unit defined in root package in file callWithError.kt

@Deprecated("", ReplaceWith("newFun()"))
fun oldFun() {
}

fun newFun(){}

fun foo() {
    <caret>oldFun(123)
}
