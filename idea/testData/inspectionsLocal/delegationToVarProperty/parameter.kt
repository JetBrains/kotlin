// PROBLEM: none
interface A {
    fun foo()
}
class B : A {
    override fun foo() {
    }
}
class C(<caret>b: B) : A by b