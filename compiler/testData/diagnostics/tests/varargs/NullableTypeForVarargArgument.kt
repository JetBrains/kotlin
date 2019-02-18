// !WITH_NEW_INFERENCE
// !DIAGNOSTICS:-UNUSED_PARAMETER

// KT-9883 prohibit using spread operator for nullable value

// FILE: A.java

public class A {
    public void foo(int x, String ... args) {}
    public static String[] ar;
}

// FILE: J.java

public class J {
    public interface Invoke {
        void invoke(String ...args);
    }

    public static Invoke staticFun;
}

// FILE: 1.kt
val args: Array<String>? = null

fun bar(x: Int, vararg s: String) {}

fun baz(s: String) {}

fun getArr(): Array<String>? = null

fun f() {
    A().foo(1, <!SPREAD_OF_NULLABLE!>*<!>args)
    bar(2, <!SPREAD_OF_NULLABLE!>*<!><!TYPE_MISMATCH!>args<!>)
    baz(<!NON_VARARG_SPREAD, SPREAD_OF_NULLABLE!>*<!><!NI;TYPE_MISMATCH!>args<!>)
}

fun g(args: Array<String>?) {

    if (args != null) {
        A().foo(1, *<!OI;DEBUG_INFO_SMARTCAST!>args<!>)
    }
    A().foo(1, *A.ar)
}

class B {
    var args: Array<String>? = null
}

fun h(b: B) {
    if (b.args != null) {
        A().foo(1, <!SPREAD_OF_NULLABLE!>*<!><!OI;SMARTCAST_IMPOSSIBLE!>b.args<!>)
    }
}

fun k() {
    A().foo(1, <!SPREAD_OF_NULLABLE!>*<!>getArr())
    bar(2, <!SPREAD_OF_NULLABLE!>*<!><!TYPE_MISMATCH!>getArr()<!>)
    baz(<!NON_VARARG_SPREAD, SPREAD_OF_NULLABLE!>*<!><!NI;TYPE_MISMATCH!>getArr()<!>)
}

fun invokeTest(goodArgs: Array<String>) {
    J.staticFun(*goodArgs)
    J.staticFun(<!SPREAD_OF_NULLABLE!>*<!>args)
}