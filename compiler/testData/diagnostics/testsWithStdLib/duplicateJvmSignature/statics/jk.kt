// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public class A {
    public static void foo() {}
    public static void baz(String s) {}
}

// FILE: K.kt

open class K : A() {
    companion object {
        <!ACCIDENTAL_OVERRIDE!>@JvmStatic
        fun foo()<!> {}
        @JvmStatic
        fun foo(i: Int) {}
        @JvmStatic
        fun baz(i: Int) {}
    }
}
