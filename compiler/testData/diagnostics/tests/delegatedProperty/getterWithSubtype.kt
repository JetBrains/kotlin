open class Base
class Derived: Base()

val a: Base by A()

class A {
  fun get(t: Any?, p: String): Derived {
    t.equals(p) // to avoid UNUSED_PARAMETER warning
    return Derived()
  }
}


