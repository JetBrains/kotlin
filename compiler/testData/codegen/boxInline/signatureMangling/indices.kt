// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt
inline fun inlineCallingIndices(): String {
    val i1 = arrayOf("one", "two").indices
    return "OK"
}

// FILE: 2.kt
fun box() = inlineCallingIndices()