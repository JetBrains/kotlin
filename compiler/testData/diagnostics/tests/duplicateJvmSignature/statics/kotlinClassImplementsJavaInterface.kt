// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public interface A {
    public String a = "";
    public static void foo() {}
    public static void baz(String s) {}
}

// FILE: K.kt

open class K : A {
    val a = ""
    fun foo() {}
    fun foo(i: Int) {}
    fun baz(i: Int) {}

    companion object {
        fun foo() {}
    }
}
