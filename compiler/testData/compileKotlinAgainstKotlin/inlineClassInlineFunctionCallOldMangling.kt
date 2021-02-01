// TARGET_BACKEND: JVM
// !LANGUAGE: +InlineClasses
// FILE: A.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME

inline class A(val x: String) {
    inline fun f(other: A): A = other
}

// FILE: B.kt

fun box(): String {
    return A("Fail").f(A("OK")).x
}
