// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  var a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var aTopLevel: Int by Delegate()

class Delegate {
  operator fun getValue(t: Nothing?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: Nothing?, p: KProperty<*>, a: Int) {
  }
}
