// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
// FILE: A.kt

inline class A private constructor(val value: String) {
    constructor(c: Char) : this(c + "K")
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String = A('O').value
