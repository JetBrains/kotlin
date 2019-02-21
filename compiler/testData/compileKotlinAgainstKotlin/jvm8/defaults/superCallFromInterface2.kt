// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: 1.kt
interface Test {
    @JvmDefault
    fun test(): String {
        return "OK"
    }

    fun defaultImplTrigger(): String {
        return "OK"
    }
}

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
