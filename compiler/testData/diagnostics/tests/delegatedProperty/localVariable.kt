// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class Local {
  fun foo() {
    val a: Int <!LOCAL_VARIABLE_WITH_DELEGATE!>by Delegate()<!>
  }
}

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}