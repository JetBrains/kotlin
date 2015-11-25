// !DIAGNOSTICS:-UNUSED_PARAMETER

// KT-9883 prohibit using spread operator for nullable value

// FILE: A.java

public class A {
    public void foo(int x, String ... args) {}
    public static String[] ar;
}


// FILE: 1.kt
val args: Array<String>? = null

fun bar(x: Int, vararg s: String) {}

fun baz(s: String) {}

fun f() {
    A().foo(1, <!SPREAD_OF_NULLABLE!>*<!>args)
    bar(2, <!SPREAD_OF_NULLABLE!>*<!><!TYPE_MISMATCH!>args<!>)
    baz(<!NON_VARARG_SPREAD, SPREAD_OF_NULLABLE!>*<!>args)
}

fun g(args: Array<String>?) {

    if (args != null) {
        A().foo(1, *<!DEBUG_INFO_SMARTCAST!>args<!>)
    }
    A().foo(1, *A.ar)
}

class B {
    var args: Array<String>? = null
}

fun h(b: B) {
    if (b.args != null) {
        A().foo(1, <!SPREAD_OF_NULLABLE!>*<!><!SMARTCAST_IMPOSSIBLE!>b.args<!>)
    }
}


