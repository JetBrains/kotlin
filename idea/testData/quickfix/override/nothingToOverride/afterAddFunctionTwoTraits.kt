// "Add function to supertype..." "true"
trait A {
    open fun foo()
}
trait B {}
class C: A, B {
  <caret>override fun foo() {}
}