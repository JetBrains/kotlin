// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base
class Derived: Base()

var a: Derived by A()

class A {
  fun getValue(t: Any?, p: PropertyMetadata): Derived {
    return Derived()
  }

  fun setValue(t: Any?, p: PropertyMetadata, i: Base) {}
}

