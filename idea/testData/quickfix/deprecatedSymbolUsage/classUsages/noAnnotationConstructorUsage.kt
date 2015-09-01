// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

@Deprecated("")
class C(p: Int)

fun foo() {
    <caret>C(1)
}
