// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: ANY
// IGNORE_HEADER_MODE: ANY
// ^^^ KT-17301: java.lang.StackOverflowError during compilation.
//     Various compiler parts are not ready for such a deep-nested trees:
//     `AnalyzerWithCompilerReport.reportSyntaxErrors()`, IR validator, various IR lowerings and backends.

// FILE: lib.kt
class A(val a: Int) {
    fun foo() = A(a + 1)
}

inline fun test(): Int {
    val v = A(0)
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo() // 20 in one line
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()


        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo() // 20 in one line
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()


        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()
        .foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo().foo()

    return v.a
}

// FILE: main.kt
fun box(): String {
    val actual = test()
    if (actual != 2000) return actual.toString()
    return "OK"
}
