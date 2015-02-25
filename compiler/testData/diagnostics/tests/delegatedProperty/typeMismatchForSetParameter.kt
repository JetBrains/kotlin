// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
  var a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
  fun set(t: Any?, p: PropertyMetadata, i: String) {}
}
