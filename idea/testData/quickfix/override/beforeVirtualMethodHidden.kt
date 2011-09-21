// "Add 'override' modifier" "true"
open class A() {
    fun foo() {}
}

class B() : A() {
    fun <caret>foo() {}
}
