// !API_VERSION: 1.3
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FILE: 1.kt
interface Test {
    @kotlin.annotations.JvmDefault
    val prop: String
        get() =  "OK"
}

// FILE: 2.kt
interface Test2 : Test {
    @kotlin.annotations.JvmDefault
    override val prop: String
        get() = super.prop
}

fun box(): String {
    return object : Test2 {}.prop
}
