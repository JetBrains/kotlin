abstract class A {
    abstract val a: Int <!ABSTRACT_DELEGATED_PROPERTY!>by Delegate()<!>
}

class Delegate {
  fun get(t: Any?, p: String): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }
}