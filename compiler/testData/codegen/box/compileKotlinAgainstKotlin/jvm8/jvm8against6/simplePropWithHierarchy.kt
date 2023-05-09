// MODULE: lib
// !JVM_DEFAULT_MODE: disable
// FILE: 1.kt

interface Test {
    val test: String
        get() = "OK"
}

// MODULE: main(lib)
// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
// WITH_STDLIB
// FILE: 2.kt
interface Test2 : Test {
    override val test: String
        get() = super.test
}


class TestClass : Test2 {

}


fun box(): String {
    return TestClass().test
}
