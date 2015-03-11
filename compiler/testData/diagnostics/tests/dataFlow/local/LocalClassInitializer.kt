// KT-338 Support.smartcasts in nested declarations

fun f(a: Any?) {
  if (a is B) {
    class C : X(<!DEBUG_INFO_SMARTCAST!>a<!>) {
      init {
        <!DEBUG_INFO_SMARTCAST!>a<!>.foo()
      }
    }
  }
}

trait B {
  fun foo() {}
}
open class X(<!UNUSED_PARAMETER!>b<!>: B)