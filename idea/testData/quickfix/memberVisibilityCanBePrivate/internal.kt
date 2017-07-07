// "Change visibility to 'private'" "true"
class A {
    <caret>internal val a = ""

    fun foo() {
        a
    }
}