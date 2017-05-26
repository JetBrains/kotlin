// "Add 'private' modifier" "true"
class A {
    internal val <caret>a = ""

    fun foo() {
        a
    }
}