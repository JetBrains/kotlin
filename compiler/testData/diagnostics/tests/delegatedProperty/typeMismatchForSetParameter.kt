// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
  var a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  operator fun getValue(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
  operator fun setValue(t: Any?, p: PropertyMetadata, i: String) {}
}
