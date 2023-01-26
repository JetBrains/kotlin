// FIR_IDENTICAL

interface Tr : <!DYNAMIC_SUPERTYPE!>dynamic<!>

fun <T: <!DYNAMIC_UPPER_BOUND!>dynamic<!>> foo() {}

class C<T> where T : <!DYNAMIC_UPPER_BOUND!>dynamic<!>