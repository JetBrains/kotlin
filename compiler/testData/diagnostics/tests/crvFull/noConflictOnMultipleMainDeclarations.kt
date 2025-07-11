// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: a.kt
fun main() {
    println("a")
}

// FILE: b.kt
fun main() {
    println("b")
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
