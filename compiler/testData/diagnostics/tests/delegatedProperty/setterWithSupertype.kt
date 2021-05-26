// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

open class Base
class Derived: Base()

var a: Derived by <!NEW_INFERENCE_ERROR, NEW_INFERENCE_ERROR!>A()<!>

class A {
  operator fun getValue(t: Any?, p: KProperty<*>): Derived {
    return Derived()
  }

  operator fun setValue(t: Any?, p: KProperty<*>, i: Base) {}
}
