// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73431
// RENDER_DIAGNOSTIC_ARGUMENTS
package stuff

fun foo(testInput: SomeTestData): TestResult {
    val unused = <!UNRESOLVED_REFERENCE("someInfo")!>someInfo<!>()
    val unused2 = testInput.testInfo().<!UNRESOLVED_REFERENCE("testData;  on receiver of type 'List<TestInfo<Boolean, HasTestResult>>'")!>testData<!>().someMethod()
    return TODO()
}
class SomeTestData {
    fun testInfo(): List<TestInfo<Boolean, HasTestResult>> { return TODO() }
}
class TestInfo<Boolean, HasTestResult> {
    fun testData(): HasTestResult { return TODO() }
}
class HasTestResult {
    fun testResult(): TestResult { return TODO() }
}
class TestResult

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, propertyDeclaration,
typeParameter */
