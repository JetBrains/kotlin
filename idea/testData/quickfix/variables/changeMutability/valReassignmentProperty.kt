// "Make variable mutable" "true"
class A() {
    val a = 1

    fun foo() {
        <caret>a = 5
    }
}
