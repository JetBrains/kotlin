//KT-2397 Prohibit final methods in traits with no implementation
package a

interface T {
    <!FINAL_FUNCTION_WITH_NO_BODY, DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> fun foo()
    <!FINAL_PROPERTY_IN_TRAIT, DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> val b : Int

    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> fun bar() {}
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> val c : Int
       get() = 42

    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>final<!> val d = <!PROPERTY_INITIALIZER_IN_TRAIT!>1<!>
}

class A {
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>final fun foo()<!>
}