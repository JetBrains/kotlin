// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base
class Derived: Base()

val a: Base by A()

class A {
  fun get(t: Any?, p: PropertyMetadata): Derived {
    return Derived()
  }
}


