// MODULE: lib
// FILE: 1.kt

interface Test {
    val test: String
        get() = "OK"
}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// FILE: 2.kt
class TestClass : Test {
    override val test: String
        get() = super.test
}

fun box(): String {
    return TestClass().test
}
