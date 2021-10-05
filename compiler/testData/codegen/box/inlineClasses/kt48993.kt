// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: 1.kt
inline class C(val x: String)

// FILE: 2.kt
@file:JvmMultifileClass
@file:JvmName("Multifile")

private var result: String? = null

var String.k: C
    get() = C(this + result!!)
    set(value) { result = value.x }

// FILE: 3.kt
fun box(): String {
    "".k = C("K")
    return "O".k.x
}
