// "Make 'a' private" "true"
class A {
    <caret>public val a = ""

    fun foo() {
        a
    }
}