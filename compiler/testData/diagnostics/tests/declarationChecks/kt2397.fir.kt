//KT-2397 Prohibit final methods in traits with no implementation
package a

interface T {
    final fun foo()
    final val b : Int

    final fun bar() {}
    final val c : Int
       get() = 42

    final val d = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
}

class A {
    final fun foo()
}