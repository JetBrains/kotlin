class B {
    val a: Int by Delegate()

    fun foo() = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
}

class Delegate {
  fun get(t: Any?, p: String): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }
}

