interface A {
    fun foo(value : String) : Unit = 0
}

class C : A {
  <caret>
}
