// FILE: issue/pack/RowIcon.java
package issue.pack;

public class RowIcon {
    public class RClass {}
}

// FILE: usage.kt
package usage

fun testJavaNestedClass(alignment: issue.pack.RowIcon.RClass) {
}

fun checkIt() {
    testJavaNestedClass(issue.pack.RowIcon().RClass())
}
