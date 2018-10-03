// "Change to var" "true"
class A(val a: Int) {
    fun foo() {
        <caret>a = 5
    }
}
