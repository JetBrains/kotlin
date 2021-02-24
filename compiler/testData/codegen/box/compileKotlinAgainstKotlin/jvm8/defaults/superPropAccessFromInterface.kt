// IGNORE_BACKEND_FIR: JVM_IR
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME
// MODULE: lib
// FILE: 1.kt
interface Test {
    @JvmDefault
    val prop: String
        get() =  "OK"
}

// MODULE: main(lib)
// FILE: 2.kt
interface Test2 : Test {
    @JvmDefault
    override val prop: String
        get() = super.prop
}

fun box(): String {
    return object : Test2 {}.prop
}
