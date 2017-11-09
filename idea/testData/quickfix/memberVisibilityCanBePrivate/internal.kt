// "Make 'a' private" "true"
class A {
    <caret>internal val a = ""

    fun foo() {
        a
    }
}