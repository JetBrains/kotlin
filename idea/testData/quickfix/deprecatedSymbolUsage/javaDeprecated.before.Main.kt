// "class org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageFix" "false"
// ACTION: Inspection 'DEPRECATED_SYMBOL_WITH_MESSAGE' options

fun foo() {
    val c = JavaClass()
    c.<caret>oldFun()
}
