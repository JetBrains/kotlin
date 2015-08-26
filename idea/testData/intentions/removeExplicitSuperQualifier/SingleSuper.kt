open class B {
    open fun foo(){}
}
class A : B() {
    override fun foo() {
        super<B><caret>.foo()
    }
}