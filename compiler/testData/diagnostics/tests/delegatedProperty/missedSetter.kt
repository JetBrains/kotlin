var a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>A()<!>

class A {
    fun get(t: Any?, p: PropertyMetadata): Int {
      t.equals(p) // to avoid UNUSED_PARAMETER warning
      return 1
    }
}
