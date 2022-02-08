// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB
// MODULE: lib
// FILE: 1.kt
interface Test {
    @JvmDefault
    fun test(): String {
        return "OK"
    }
}

// MODULE: main(lib)
// FILE: 2.kt

interface Test2 : Test {
    @JvmDefault
    override fun test(): String {
        return super.test()
    }
}

fun box(): String {
    return object : Test2 {}.test()
}
