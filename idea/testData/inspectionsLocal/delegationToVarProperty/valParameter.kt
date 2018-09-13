// PROBLEM: none
interface A {
    fun foo()
}
class B : A {
    override fun foo() {}
}
class C(<caret>val b: B) : A by b