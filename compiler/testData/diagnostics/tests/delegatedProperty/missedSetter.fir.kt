// RUN_PIPELINE_TILL: SOURCE
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var a: Int <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> A()

class A {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
      return 1
    }
}
