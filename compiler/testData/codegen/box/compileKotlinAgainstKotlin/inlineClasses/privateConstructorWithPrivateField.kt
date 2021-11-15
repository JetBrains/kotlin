// WITH_STDLIB
// MODULE: lib
// FILE: A.kt

inline class A private constructor(private val value: String) {
    constructor(c: Char) : this(c + "K")

    val publicValue: String get() = value
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String = A('O').publicValue
