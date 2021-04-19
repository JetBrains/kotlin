// JAVAC_EXPECTED_FILE
// FILE: foo/A.java
package foo;

public class A {
    static void f(B b) {
        b.g();
    }

    public interface B {
        void g();
    }
}

// FILE: bar/sample.kt

package bar

fun main() {
    foo.A.<!INVISIBLE_REFERENCE!>f<!> {}
}
