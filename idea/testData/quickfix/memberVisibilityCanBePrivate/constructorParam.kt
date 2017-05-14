// "Add 'private' modifier" "true"
class A(internal val <caret>a: String = "") {
    fun foo() {
        a
    }
}
