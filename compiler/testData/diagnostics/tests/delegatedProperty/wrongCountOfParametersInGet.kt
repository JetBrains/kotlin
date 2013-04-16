class A {
  val a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

val aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  fun get(t: Any?, p: String, a: Int): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return a
  }
}
