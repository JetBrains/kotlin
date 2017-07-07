// "Add 'private' modifier" "true"
class A {
    <caret>internal val a = ""

    fun foo() {
        a
    }
}