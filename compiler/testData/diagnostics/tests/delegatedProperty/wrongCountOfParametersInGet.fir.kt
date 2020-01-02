// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class A {
  val a: Int by Delegate()
}

val aTopLevel: Int by Delegate()

class Delegate {
  fun getValue(t: Any?, p: KProperty<*>, a: Int): Int {
    return a
  }
}
