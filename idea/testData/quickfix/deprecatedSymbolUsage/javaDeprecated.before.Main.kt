// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

fun foo() {
    val c = JavaClass()
    c.<caret>oldFun()
}
