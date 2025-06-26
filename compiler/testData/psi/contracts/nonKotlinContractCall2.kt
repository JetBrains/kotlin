// FILE: Test.kt
class Test

// FILE: main.kt
fun testBuilder(id: String = "", lambda: Test.() -> Unit) = Test()

fun test() {
    contract(testBuilder {})
}

fun contract(test: Test) {}
