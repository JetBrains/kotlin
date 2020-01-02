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
    companion object {
        @JvmStatic
        fun foo(i: Int) {}
        @JvmStatic
        fun bar(d: Double) {}
    }
}
