// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

open class Base
class Derived: Base()

val a: Base by A()

class A {
  operator fun getValue(t: Any?, p: KProperty<*>): Derived {
    return Derived()
  }
}

