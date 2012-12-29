//KT-2397 Prohibit final methods in traits with no implementation
package a

trait T {
    <!FINAL_FUNCTION_WITH_NO_BODY!>final<!> fun foo()
    <!FINAL_PROPERTY_IN_TRAIT!>final<!> val b : Int

    final fun bar() {}
    final val c : Int
       get() = 42

    final val d = <!PROPERTY_INITIALIZER_IN_TRAIT!>1<!>
}

class A {
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>final fun foo()<!>
}