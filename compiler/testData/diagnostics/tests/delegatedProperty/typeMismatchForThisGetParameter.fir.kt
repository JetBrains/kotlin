// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

import kotlin.reflect.KProperty

class B {
  val b: Int by Delegate()
}

val bTopLevel: Int by Delegate()

class A

class Delegate {
  fun getValue(t: A, p: KProperty<*>): Int {
    return 1
  }
}
