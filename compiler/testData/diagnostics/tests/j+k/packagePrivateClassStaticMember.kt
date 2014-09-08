// KT-4021 Java's Package visibilty does not work for static methods

// FILE: foo/Bar.java

package foo;

class Bar {
    static void baz() {}
}

// FILE: main.kt

package foo

fun main() {
    Bar.baz()
}
