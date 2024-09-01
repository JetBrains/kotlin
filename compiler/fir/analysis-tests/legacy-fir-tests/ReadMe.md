# FIR Analysis tests

Mostly, format of FIR analysis test is the same as the one described at `compiler/testData/diagnostics/ReadMe.md`.


`DEBUG_INFO_EXPRESSION_TYPE` and `DEBUG_INFO_CALL` are also partially supported but need to be written out explicitly in a test data file:
```kotlin
fun foo(
    x1: String,
    x2: Collection<CharSequence>,
    x3: MutableMap<out CharSequence, in MutableList<*>>
) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Collection<kotlin.CharSequence>")!>x2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableMap<out kotlin.CharSequence, in kotlin.collections.MutableList<*>>")!>x3<!>
}

interface B {
    operator fun invoke(x: Int): String
}

class A {
    fun foo(x: Int) {
        fun baz(x: Double) {}
        <!DEBUG_INFO_CALL("fqName: A.foo.baz; typeCall: function")!>baz(1.0)<!>
    }

    val bar: B = TODO()
}

fun A.foo(x: String) {}

fun main() {
    fun A.foo(x: Double) {}
    val a = A()
    a.<!DEBUG_INFO_CALL("fqName: A.foo; typeCall: function")!>foo(1)<!>
    a.<!DEBUG_INFO_CALL("fqName: foo; typeCall: extension function")!>foo("")<!>
    a.<!DEBUG_INFO_CALL("fqName: main.foo; typeCall: extension function")!>foo(1.0)<!>
    a.<!DEBUG_INFO_CALL("fqName: B.invoke; typeCall: variable&invoke")!>bar(1)<!>
}
```

## Specific Directives
- `// FIR_IDENTICAL` should be prepended in a `.kt` file of an old diagnostic test when analysis result of old FE and FIR are the same
- `// FIR_IGNORE` should be prepended in a `.fir.kt` file of an old diagnostic test known to fail with exception
- `// DUMP_CFG` generates `.dot` file with rendered data flow graph (see comment at `org.jetbrains.kotlin.fir.AbstractFirDiagnosticsTest`) 
