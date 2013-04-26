class Local {
  fun foo() {
    val <!UNUSED_VARIABLE!>a<!>: Int <!LOCAL_VARIABLE_WITH_DELEGATE!>by Delegate()<!>
  }
}

class Delegate {
  fun get(t: Any?, p: String): Int {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return 1
  }
}