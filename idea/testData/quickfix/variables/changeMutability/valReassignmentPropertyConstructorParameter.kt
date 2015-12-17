// "Make variable mutable" "true"
class A(val a: Int) {
    fun foo() {
        <caret>a = 5
    }
}
