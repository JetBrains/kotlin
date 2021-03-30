// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

import kotlin.reflect.KProperty

class A {
  var a: Int by <!ARGUMENT_TYPE_MISMATCH!>Delegate()<!>
}

var aTopLevel: Int by Delegate()

class Delegate {
  operator fun getValue(t: Nothing?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: Nothing?, p: KProperty<*>, a: Int) {
  }
}
