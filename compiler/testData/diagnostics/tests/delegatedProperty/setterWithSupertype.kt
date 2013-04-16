open class Base
class Derived: Base()

var a: Derived by A()

class A {
  fun get(t: Any?, p: String): Derived {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return Derived()
  }

  fun set(t: Any?, p: String, i: Base) {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    i.equals(null) // to avoid UNUSED_PARAMETER warning
  }
}

