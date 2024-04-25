// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

var a: Int by <!DELEGATE_SPECIAL_FUNCTION_MISSING("setValue(Nothing?, KProperty<*>, Int); A; delegate for var (read-write property)")!>A()<!>

class A {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
      return 1
    }
}
