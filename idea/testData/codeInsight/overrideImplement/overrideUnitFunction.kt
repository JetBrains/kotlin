// FIR_IDENTICAL
interface A {
    fun foo(value : String) : Unit {}
}

class C : A {
  <caret>
}
