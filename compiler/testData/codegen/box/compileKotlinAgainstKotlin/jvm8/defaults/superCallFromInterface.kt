// !JVM_DEFAULT_MODE: all
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// FILE: 1.kt
interface Test {
    fun test(): String {
        return "OK"
    }
}

// MODULE: main(lib)
// FILE: 2.kt

interface Test2 : Test {
    override fun test(): String {
        return super.test()
    }
}

fun box(): String {
    return object : Test2 {}.test()
}
