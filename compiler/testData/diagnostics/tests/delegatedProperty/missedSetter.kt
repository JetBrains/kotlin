// !DIAGNOSTICS: -UNUSED_PARAMETER

var a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>A()<!>

class A {
    fun get(t: Any?, p: PropertyMetadata): Int {
      return 1
    }
}
