// WITH_RUNTIME
// MODULE: lib
// FILE: A.kt

inline class A private constructor(val value: String) {
    constructor(c: Char) : this(c + "K")
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String = A('O').value
