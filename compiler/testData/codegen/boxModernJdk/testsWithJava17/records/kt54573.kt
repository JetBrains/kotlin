// TARGET_BACKEND: JVM
// ISSUE: KT-54573
// WITH_STDLIB

@JvmRecord
data class A constructor(val x: Int, val s: String) {
    constructor(s: String) : this(s.length, s)
}

fun box(): String = A("OK").s
