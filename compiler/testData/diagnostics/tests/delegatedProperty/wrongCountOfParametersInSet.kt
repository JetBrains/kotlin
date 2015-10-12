// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  var a: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var aTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }

  operator fun setValue(t: Any?, p: KProperty<*>, a: Int, c: Int) {}
}
