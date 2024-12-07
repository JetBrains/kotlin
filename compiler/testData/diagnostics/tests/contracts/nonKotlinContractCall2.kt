// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// DUMP_IR
// ISSUE: KT-71732

class Test

fun testBuilder(id: String = "", lambda: Test.() -> Unit) = Test()

fun test() {
    contract(testBuilder {})
}

fun contract(test: Test) {}
