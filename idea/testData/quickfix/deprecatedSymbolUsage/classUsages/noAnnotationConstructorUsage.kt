// "class org.jetbrains.kotlin.idea.quickfix.DeprecatedSymbolUsageFix" "false"

@deprecated("")
class C(p: Int)

fun foo() {
    <caret>C(1)
}
