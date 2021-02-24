// FIR_COMPARISON
class TestClass {
    public fun testMethod() {
    }
}

fun testFun() {
    val lambda = { -> TestClass() }
    lambda().<caret>
}

// EXIST: testMethod