// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

import kotlin.reflect.KProperty

class D {
  var c: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

var cTopLevel: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>

class A

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: A, p: KProperty<*>, i: Int) {}
}
