// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base
class Derived: Base()

val a: Base by A()

class A {
  operator fun getValue(t: Any?, p: PropertyMetadata): Derived {
    return Derived()
  }
}


