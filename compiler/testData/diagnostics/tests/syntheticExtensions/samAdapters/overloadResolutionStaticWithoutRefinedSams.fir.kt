// !LANGUAGE: -RefinedSamAdaptersPriority -NewInference
// !CHECK_TYPE
// FILE: A.java
public class A {
    public static int foo(Runnable r) { return 0; }
    public static String foo(Object r) { return null;}

    public static int bar(Runnable r) { return 1; }
    public static String bar(CharSequence r) { return null; }
}

// FILE: 1.kt
fun fn() {}
fun x(r: Runnable) {
    A.foo(::fn) checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    A.foo {} checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    A.foo(null) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    A.foo(Runnable { }) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    A.foo(r) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    A.foo(123) checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    A.foo("") checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    A.bar(::fn) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    A.bar {} checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    A.bar(r) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }

    A.<!AMBIGUITY!>bar<!>(null)

    A.bar(null as Runnable?) checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    A.bar(null as CharSequence?) checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    A.bar("") checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    A.<!INAPPLICABLE_CANDIDATE!>bar<!>(123)
}
