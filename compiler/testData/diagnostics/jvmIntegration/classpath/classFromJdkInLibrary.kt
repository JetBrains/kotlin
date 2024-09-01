// FIR_IDENTICAL
// MODULE: library
// FILE: java/util/Date.java
package java.util;

public class Date {
    public static void methodWhichDoesNotExistInJdk() {}
}

// MODULE: main(library)
// FILE: source.kt
import java.util.Date

fun foo() {
    Date.<!UNRESOLVED_REFERENCE!>methodWhichDoesNotExistInJdk<!>()
}
