val Int.a by Delegate(<!NO_THIS!>this<!>)

class A {
  val Int.a by Delegate(<!TYPE_MISMATCH!>this<!>)
}

class Delegate(i: Int) {
  fun get(t: Any?, p: String): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }
}