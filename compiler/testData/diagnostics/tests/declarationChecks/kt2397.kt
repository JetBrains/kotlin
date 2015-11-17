//KT-2397 Prohibit final methods in traits with no implementation
package a

interface T {
    <!FINAL_FUNCTION_WITH_NO_BODY, WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> fun foo()
    <!FINAL_PROPERTY_IN_INTERFACE, WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> val b : Int

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> fun bar() {}
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> val c : Int
       get() = 42

    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>final<!> val d = <!PROPERTY_INITIALIZER_IN_INTERFACE!>1<!>
}

class A {
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>final fun foo()<!>
}