// KT-338 Support autocasts in nested declarations

fun f(a: Any?) {
  if (a is B) {
    class C : X(a) {
      {
        a.foo()
      }
    }
  }
}

trait B {
  fun foo() {}
}
open class X(b: B)