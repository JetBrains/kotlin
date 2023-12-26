// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-63984

// MODULE: m1
// FILE: f1.kt

object O {
    var prop: String
        external get
        external set
}

// MODULE: m2(m1)
// FILE: f2.kt

fun box(): String {
    O.hashCode() // Necessary to deserialize O
    return "OK"
}
