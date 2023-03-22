// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// ISSUE: KT-56678

class A {
    val b = <!UNINITIALIZED_VARIABLE!>a<!>
    val a = 1
    val c = a
}

class B {
    val b = <!UNINITIALIZED_VARIABLE!>a<!>
    val a: Int
    val c = <!UNINITIALIZED_VARIABLE!>a<!>
    init {
        a = 1
    }
    val d = a
}
