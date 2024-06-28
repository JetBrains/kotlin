// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: issue/pack/RowIcon.java
package issue.pack;

public class RowIcon {
    public static class RClass {}
}

// MODULE: main(lib)
// FILE: usage.kt
package usage

fun testJavaNestedClass(alignment: issue.pack.RowIcon.RClass) {
}

fun checkIt() {
    testJavaNestedClass(issue.pack.RowIcon.RClass())
}
