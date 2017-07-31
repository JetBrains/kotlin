// "Make 'a' private" "true"
class A(<caret>internal val a: String = "") {
    fun foo() {
        a
    }
}
