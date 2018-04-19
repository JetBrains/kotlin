// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: 1.kt
interface Test {
    @JvmDefault
    val prop: String
        get() =  "OK"
}

// FILE: 2.kt
interface Test2 : Test {
    @JvmDefault
    override val prop: String
        get() = super.prop
}

fun box(): String {
    return object : Test2 {}.prop
}
