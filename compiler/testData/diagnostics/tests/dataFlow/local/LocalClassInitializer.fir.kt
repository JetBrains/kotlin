// KT-338 Support.smartcasts in nested declarations

fun f(a: Any?) {
  if (a is B) {
    class C : X(a) {
      init {
        a.foo()
      }
    }
  }
}

interface B {
  fun foo() {}
}
open class X(b: B)
