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
    A.foo(::fn) checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }
    A.foo {} checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><String>() }

    A.foo(null) checkType { _<Int>() }
    A.foo(Runnable { }) checkType { _<Int>() }
    A.foo(r) checkType { _<Int>() }

    A.foo(123) checkType { _<String>() }
    A.foo("") checkType { _<String>() }

    A.bar(::fn) checkType { _<Int>() }
    A.bar {} checkType { _<Int>() }

    A.bar(r) checkType { _<Int>() }

    A.<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(null)

    A.bar(null as Runnable?) checkType { _<Int>() }
    A.bar(null as CharSequence?) checkType { _<String>() }

    A.bar("") checkType { _<String>() }
    A.<!NONE_APPLICABLE!>bar<!>(123)
}
