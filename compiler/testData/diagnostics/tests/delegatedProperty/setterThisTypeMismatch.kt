// !DIAGNOSTICS: -UNUSED_PARAMETER

class D {
  var c: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var cTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class A

class Delegate {
  fun get(t: Any?, p: PropertyMetadata): Int {
    return 1
  }
  fun set(t: A, p: PropertyMetadata, i: Int) {}
}
