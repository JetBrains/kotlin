// From KT-1254
trait T {
    fun Foo() : (String) -> Unit
}

class C : T {
  <caret>
}