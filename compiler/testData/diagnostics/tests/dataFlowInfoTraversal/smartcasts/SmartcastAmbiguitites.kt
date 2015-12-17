interface B {
  fun bar() {}
}

class C() {
  fun bar() {
  }
}

fun test(a : Any?) {
  if (a is B) {
      if (a is C) {
          <!DEBUG_INFO_SMARTCAST!>a<!>.bar();
      }
  }
}
