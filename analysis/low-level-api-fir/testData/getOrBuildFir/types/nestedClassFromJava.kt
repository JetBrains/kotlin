// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: dep
// FILE: issue/pack/RowIcon.java
package issue.pack;

public class RowIcon {
    public static class RClass {}
}

// MODULE: main(dep)
// FILE: usage.kt
package usage

fun testJavaNestedClass(alignment: <expr>issue.pack.RowIcon.RClass</expr>) {
}
