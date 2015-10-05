// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base
class Derived: Base()

val a: Base by A()

class A {
  fun getValue(t: Any?, p: PropertyMetadata): Derived {
    return Derived()
  }
}


