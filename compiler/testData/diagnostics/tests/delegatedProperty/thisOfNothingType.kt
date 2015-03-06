class A {
  var a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  fun get(t: Nothing, p: PropertyMetadata): Int {
    p.equals(null) // to avoid UNUSED_PARAMETER warning
    return 1
  }
  fun set(t: Nothing, p: PropertyMetadata, a: Int) {
    p.equals(a) // to avoid UNUSED_PARAMETER warning
  }
}