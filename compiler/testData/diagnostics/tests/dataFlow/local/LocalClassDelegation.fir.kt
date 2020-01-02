interface D {
  fun foo() {}
}

fun test(d: Any?) {
  if (d !is D) return

  class Local : D by d {
  }
}

