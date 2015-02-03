// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
  val a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

val aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  fun get(t: Any?, p: PropertyMetadata, a: Int): Int {
    return a
  }
}
