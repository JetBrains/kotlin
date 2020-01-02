// !CHECK_TYPE
// FILE: A.java
public class A {
    public int foo(Runnable r) { return 0; }
    public String foo(Object r) { return null;}

    public int bar(Runnable r) { return 1; }
    public String bar(CharSequence r) { return null; }
}

// FILE: 1.kt
fun fn() {}
fun x(a: A, r: Runnable) {
    a.foo(::fn) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.foo {} checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    a.foo(null) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.foo(Runnable { }) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.foo(r) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    a.foo(123) checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    a.foo("") checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    a.bar(::fn) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.bar {} checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    a.bar(r) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    a.<!AMBIGUITY!>bar<!>(null)

    a.bar(null as Runnable?) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    a.bar(null as CharSequence?) checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    a.bar("") checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    a.<!INAPPLICABLE_CANDIDATE!>bar<!>(123)
}
