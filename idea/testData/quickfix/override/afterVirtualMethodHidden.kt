// "Add 'override' modifier" "true"
open class A() {
    fun foo() {}
}

class B() : A() {
    override fun <caret>foo() {}
}
