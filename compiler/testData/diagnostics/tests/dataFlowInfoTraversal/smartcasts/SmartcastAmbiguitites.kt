interface B {
  fun bar() {}
}

class C() {
  fun bar() {
  }
}

fun test(a : Any?) {
  if (a is B) {
      if (<!USELESS_IS_CHECK!>a is C<!>) {
          <!DEBUG_INFO_SMARTCAST!>a<!>.bar();
      }
  }
}
