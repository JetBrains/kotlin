// IGNORE_BACKEND_MULTI_MODULE: JVM_IR_SERIALIZE
// MODULE: lib
// FILE: lib.kt

// KT-65503
inline fun func(str: String): String {
    val obj = object {
        fun f0() =
            object {
                fun f1(): String {
                    return str
                }
            }
    }

    return obj.f0().f1()
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return func("OK")
}