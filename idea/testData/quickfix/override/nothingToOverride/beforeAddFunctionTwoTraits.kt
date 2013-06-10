// "Add function to supertype..." "true"
trait A {}
trait B {}
class C: A, B {
  <caret>override fun foo() {}
}