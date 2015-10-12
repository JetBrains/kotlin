// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING(setValue\(kotlin.Nothing?, kotlin.reflect.KProperty<*>, kotlin.Int\); A)!>A()<!>

class A {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
      return 1
    }
}
