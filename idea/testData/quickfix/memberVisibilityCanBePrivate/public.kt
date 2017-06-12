// "Add 'private' modifier" "true"
class A {
    public val <caret>a = ""

    fun foo() {
        a
    }
}