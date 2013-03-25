// FILE: vvv/A.java
package vvv;

public enum A {
    ENTRY,
    ANOTHER;

    public String ENTRY = "";
}

// FILE: test.kt

package vvv

fun main() {
    val c: A = A.ENTRY
    val <!UNUSED_VARIABLE!>c2<!>: String? = c.ENTRY
    val <!UNUSED_VARIABLE!>c3<!>: String? = A.ANOTHER.ENTRY
}
