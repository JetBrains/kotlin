// MODULE: dep
// FILE: issue/pack/RowIcon.java
package issue.pack;

public class RowIcon {
    public static class RClass {}
}

// MODULE: main(dep)
// FILE: usage.kt
package usage

fun testJavaNestedClass(alignment: issue.pack.RowIcon.RClass) {
}

fun checkIt() {
    testJavaNestedClass(issue.pack.RowIcon.RClass())
}
