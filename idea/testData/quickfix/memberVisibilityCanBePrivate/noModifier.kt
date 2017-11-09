// "Make 'a' private" "true"
class A {
    val <caret>a = ""

    fun foo() {
        a
    }
}