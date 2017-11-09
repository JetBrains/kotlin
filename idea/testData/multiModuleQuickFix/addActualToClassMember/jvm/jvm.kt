// "Add 'actual' modifier" "true"
// ERROR: Declaration should be marked with 'actual' (suppress with -Xno-check-actual)

actual class Foo {
    fun <caret>foo() {}
}
