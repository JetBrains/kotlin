// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public interface A {
    public static void foo() {}
    public static void baz(String s) {}
}

// FILE: B.java

public interface B extends A {
    public static void bar(int i) {}
}

// FILE: K.kt

open class K : B {
    fun foo() {}
    fun foo(a: Any) {}
    fun bar(i: Int) {}
    fun bar(i: String) {}
    fun baz(i: Int) {}

    companion object {
        fun foo() {}
        fun bar(i: Int) {}
    }
}
