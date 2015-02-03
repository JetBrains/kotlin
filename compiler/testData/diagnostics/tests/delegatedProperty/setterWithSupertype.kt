// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base
class Derived: Base()

var a: Derived by A()

class A {
  fun get(t: Any?, p: PropertyMetadata): Derived {
    return Derived()
  }

  fun set(t: Any?, p: PropertyMetadata, i: Base) {}
}

