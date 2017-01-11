// FILE: 1.kt

interface Test {
    val test: String
        get() = "OK"
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS
class TestClass : Test {
    override val test: String
        get() = super.test
}

fun box(): String {
    return TestClass().test
}
