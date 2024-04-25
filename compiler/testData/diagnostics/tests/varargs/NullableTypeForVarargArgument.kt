// DIAGNOSTICS:-UNUSED_PARAMETER

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
    baz(<!NON_VARARG_SPREAD_ERROR, SPREAD_OF_NULLABLE!>*<!><!TYPE_MISMATCH!>args<!>)
}

fun g(args: Array<String>?) {

    if (args != null) {
        A().foo(1, *args)
    }
    A().foo(1, *A.ar)
}

class B {
    var args: Array<String>? = null
}

fun h(b: B) {
    if (b.args != null) {
        A().foo(1, <!SPREAD_OF_NULLABLE!>*<!>b.args)
    }
}

fun k() {
    A().foo(1, <!SPREAD_OF_NULLABLE!>*<!>getArr())
    bar(2, <!SPREAD_OF_NULLABLE!>*<!><!TYPE_MISMATCH!>getArr()<!>)
    baz(<!NON_VARARG_SPREAD_ERROR, SPREAD_OF_NULLABLE!>*<!><!TYPE_MISMATCH!>getArr()<!>)
}

fun invokeTest(goodArgs: Array<String>) {
    J.staticFun(*goodArgs)
    J.staticFun(<!SPREAD_OF_NULLABLE!>*<!>args)
    J.staticFun(<!SPREAD_OF_NULLABLE!>*<!>args <!USELESS_ELVIS_RIGHT_IS_NULL!>?: null<!>)
}
