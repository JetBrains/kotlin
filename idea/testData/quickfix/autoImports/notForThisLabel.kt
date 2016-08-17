// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ERROR: Unresolved reference: @String

fun refer() {
    val v1 = this@String<caret>
}
