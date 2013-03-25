// FILE: vvv/A.java
package vvv;

public enum A {
    ENTRY("1"),
    ANOTHER("2");

    public String s;

    private A(String s) {
        this.s = s;
    }
}

// FILE: test.kt

package vvv

fun main() {
    val c = A.ENTRY
    c.s
    A.ANOTHER.s
}
