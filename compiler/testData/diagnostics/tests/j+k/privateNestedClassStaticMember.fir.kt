// KT-4149 static members of Java private nested class are accessible from Kotlin

// FILE: javaPackage/Foo.java

package javaPackage;

public class Foo {
    private static class Bar {
        public static void doSmth() {
        }
    }
}

// FILE: 1.kt

fun main() {
    javaPackage.Foo.<!INVISIBLE_REFERENCE, NO_COMPANION_OBJECT!>Bar<!>.<!INVISIBLE_REFERENCE!>doSmth<!>()
}
