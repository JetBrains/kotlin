// TARGET_BACKEND: JVM
// ^^^ Only JVM allows `main` functions in the same package but in different files.

// FIR_IDENTICAL
// WITH_STDLIB

// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ The main function has a different mangled name when it's computed from its K1 descriptor in K/JVM.

// FILE: a.kt
fun main() {
    println("main() in a.kt")
}

// FILE: b.kt
fun main() {
    println("main() in b.kt")
}

// FILE: c.kt
package foo

fun main() {
    println("foo.main() in c.kt")
}

// FILE: d.kt
package foo

fun main() {
    println("foo.main() in d.kt")
}
