// FIX: Remove var
interface A {
    fun foo()
}
class B : A {
    override fun foo() {}
}
class C(var b: B<caret>) : A by b