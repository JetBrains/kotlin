// TARGET_BACKEND: JVM

// CHECK_BYTECODE_TEXT
// 0 ATHROW
// -- invoking throwUninitializedPropertyAccessException is enough

class Test {
    private lateinit var z: String

    fun test(): String {
        z = "OK"
        return z
    }
}

fun box(): String {
    return Test().test()
}
