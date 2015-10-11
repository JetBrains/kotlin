// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class Local {
  fun foo() {
    val a: Int <!LOCAL_VARIABLE_WITH_DELEGATE!>by Delegate()<!>
  }
}

class Delegate {
  fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
}