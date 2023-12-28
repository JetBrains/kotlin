// TARGET_BACKEND: JVM_IR

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
