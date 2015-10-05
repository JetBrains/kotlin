// !DIAGNOSTICS: -UNUSED_PARAMETER

var a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING(setValue\(kotlin.Nothing?, kotlin.PropertyMetadata, kotlin.Int\); A)!>A()<!>

class A {
    fun getValue(t: Any?, p: PropertyMetadata): Int {
      return 1
    }
}
