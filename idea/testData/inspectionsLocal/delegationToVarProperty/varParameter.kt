// FIX: Change to val
interface A {
    fun foo()
}
class B : A {
    override fun foo() {}
}
class C(<caret>var b: B) : A by b