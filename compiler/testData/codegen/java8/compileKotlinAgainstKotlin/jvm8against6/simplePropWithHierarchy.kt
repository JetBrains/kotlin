// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// FILE: 1.kt

interface Test {
    val test: String
        get() = "OK"
}

// FILE: 2.kt
// JVM_TARGET: 1.8
// WITH_RUNTIME
interface Test2 : Test {
    @JvmDefault
    override val test: String
        get() = super.test
}


class TestClass : Test2 {

}


fun box(): String {
    return TestClass().test
}
