// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

import kotlin.reflect.KProperty

class A {
  var a: Int by <!INAPPLICABLE_CANDIDATE!>Delegate()<!>
}

var aTopLevel: Int by <!INAPPLICABLE_CANDIDATE!>Delegate()<!>

class Delegate {
  fun getValue(t: Nothing, p: KProperty<*>): Int {
    return 1
  }
  fun setValue(t: Nothing, p: KProperty<*>, a: Int) {
  }
}
