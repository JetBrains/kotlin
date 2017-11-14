// TARGET_BACKEND: JVM
// FILE: text.kt
import p.*

fun box() = J.foo(J.Derived())

// FILE: p/J.java
package p;

public class J {
    static class PackagePrivate {}

    public static class Derived extends PackagePrivate {}

    public static String foo(PackagePrivate pp) {
        return "OK";
    }
}
