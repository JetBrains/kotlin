// FIR_IDENTICAL
interface A {
    fun foo(value : String) : Int = 0
    fun bar() : String = "hello"
}

class C : A {
    <caret>
}
