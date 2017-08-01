// "Change visibility to 'private'" "true"
class A {
    <caret>public val a = ""

    fun foo() {
        a
    }
}