// TARGET_BACKEND: JVM
// !LANGUAGE: +InlineClasses
// FILE: A.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME

package a

inline class S(val value: String) {
    inline val k: String
        get() = value + "K"
}

// FILE: B.kt

fun box(): String {
    return a.S("O").k
}
