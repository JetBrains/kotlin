// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: foo/A.java

package foo;

public class A {
    private static void foo(int s) {}
    static void bar(double s) {}
}

// FILE: K.kt
import foo.A

open class K : A() {
    fun foo(i: Int) {}
    fun bar(d: Double) {}
    fun baz(i: Int) {}

    companion object {
        fun foo(i: Int) {}
        fun bar(d: Double) {}
    }
}
