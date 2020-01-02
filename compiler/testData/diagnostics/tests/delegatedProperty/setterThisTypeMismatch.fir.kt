// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

import kotlin.reflect.KProperty

class D {
  var c: Int by Delegate()
}

var cTopLevel: Int by Delegate()

class A

class Delegate {
  operator fun getValue(t: Any?, p: KProperty<*>): Int {
    return 1
  }
  operator fun setValue(t: A, p: KProperty<*>, i: Int) {}
}
