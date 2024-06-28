// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: dep
// FILE: issue/pack/RowIcon.kt
package issue.pack;

class RowIcon {
    class RClass
}

// MODULE: main(dep)
// FILE: usage.kt
package usage

fun testJavaNestedClass(alignment: <expr>issue.pack.RowIcon.RClass</expr>) {
}
