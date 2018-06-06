// FILE: 1.kt

interface Test {
    val test: String
        get() = "OK"
}

// FILE: 2.kt
// JVM_TARGET: 1.8
class TestClass : Test {
    override val test: String
        get() = super.test
}

fun box(): String {
    return TestClass().test
}
