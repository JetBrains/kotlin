// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base
class Derived: Base()

var a: Derived by A()

class A {
  operator fun getValue(t: Any?, p: PropertyMetadata): Derived {
    return Derived()
  }

  operator fun setValue(t: Any?, p: PropertyMetadata, i: Base) {}
}

