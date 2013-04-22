package testing.kt

class TestFromJava() : BaseJava() {
    override fun testMethod() {
    }
}

fun test() {
    BaseJava().testMethod<caret>()
}

// REF: (in testing.kt.TestFromJava).testMethod()
// REF: (in BaseJava).testMethod()
