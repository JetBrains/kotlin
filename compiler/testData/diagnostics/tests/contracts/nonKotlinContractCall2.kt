// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-71732

class Test

fun testBuilder(id: String = "", lambda: Test.() -> Unit) = Test()

fun test() {
    contract(testBuilder {})
}

fun contract(test: Test) {}
