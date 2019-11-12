// !LANGUAGE: +InlineClasses
// FILE: A.kt

package a

inline class S(val value: String) {
    inline val k: String
        get() = value + "K"
}

// FILE: B.kt

fun box(): String {
    return a.S("O").k
}
