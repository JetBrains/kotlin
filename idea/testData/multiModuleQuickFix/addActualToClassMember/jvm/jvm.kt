// "Add 'actual' modifier" "true"
// ERROR: Declaration must be marked with 'actual'

actual class Foo {
    fun <caret>foo() {}
}
