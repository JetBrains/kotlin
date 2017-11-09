// "Make 'a' private" "true"
open class A {
    <caret>protected val a = ""

    fun foo() {
        a
    }
}