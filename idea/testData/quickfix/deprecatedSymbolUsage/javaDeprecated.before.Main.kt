// "class org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageFix" "false"

fun foo() {
    val c = JavaClass()
    c.<caret>oldFun()
}
