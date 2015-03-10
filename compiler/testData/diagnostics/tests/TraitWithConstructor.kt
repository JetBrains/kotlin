class C(val a: String) {}

trait T1<!CONSTRUCTOR_IN_TRAIT!>(val x: String)<!> {}

trait T2<!CONSTRUCTOR_IN_TRAIT!>()<!> {}

trait T3<!CONSTRUCTOR_IN_TRAIT!>(<!UNUSED_PARAMETER!>a<!>: Int)<!> {}