open class Base
class Derived: Base()

var a: Derived by A()

class A {
  fun get(t: Any?, p: PropertyMetadata): Derived {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return Derived()
  }

  fun set(t: Any?, p: PropertyMetadata, i: Base) {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    i.equals(null) // to avoid UNUSED_PARAMETER warning
  }
}

