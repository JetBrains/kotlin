// "Add 'private' modifier" "true"
class A {
    <caret>public val a = ""

    fun foo() {
        a
    }
}