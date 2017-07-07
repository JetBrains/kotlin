// "Add 'private' modifier" "true"
class A(<caret>internal val a: String = "") {
    fun foo() {
        a
    }
}
