val a: Int by A(1)

class A<T: Any>(i: T) {
  fun get(t: Any?, p: String): T {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    throw Exception()
  }
}

