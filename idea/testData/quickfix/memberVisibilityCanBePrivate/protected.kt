// "Add 'private' modifier" "true"
open class A {
    protected val <caret>a = ""

    fun foo() {
        a
    }
}