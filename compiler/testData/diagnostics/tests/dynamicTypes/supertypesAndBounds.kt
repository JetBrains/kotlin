// MODULE[js]: m1
// FILE: k.kt

trait Tr : <!DYNAMIC_SUPERTYPE!>dynamic<!>

fun <T: <!DYNAMIC_UPPER_BOUND!>dynamic<!>> foo() {}

class C<T> where T : <!DYNAMIC_UPPER_BOUND!>dynamic<!>