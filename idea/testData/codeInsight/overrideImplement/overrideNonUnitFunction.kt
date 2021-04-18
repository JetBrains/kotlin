// FIR_IDENTICAL
interface A {
    fun foo(value : String) : Int = 0
}

class C : A {
  <caret>
}
