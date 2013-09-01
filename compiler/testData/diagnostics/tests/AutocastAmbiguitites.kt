trait B {
  fun bar() {}
}

class C() {
  fun bar() {
  }
}

fun test(a : Any?) {
  if (a is B) {
      if (a is C) {
          a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>();
      }
  }
}
