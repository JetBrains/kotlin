// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public class A {
    public static int a = 1;
    public static void foo() {}
    public static void baz(String s) {}
}

// FILE: K.kt

open class K : A() {
    val a = 1
    <!ACCIDENTAL_OVERRIDE!>fun foo() {}<!>
    fun foo(i: Int) {}
    fun baz(i: Int) {}

    companion object {
        fun foo() {}
    }
}
