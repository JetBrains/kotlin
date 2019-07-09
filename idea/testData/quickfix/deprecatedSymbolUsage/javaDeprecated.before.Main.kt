// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"
// ACTION: Convert to run
// ACTION: Convert to with
// ACTION: Convert to also
// ACTION: Convert to apply

fun foo() {
    val c = JavaClass()
    c.<caret>oldFun()
}
