// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

// FILE: A.kt
enum class A {
    @Deprecated("")
    DeprecatedEntry,
    RegularEntry
}

// FILE: use.kt
fun use() {
    A.<!DEPRECATION!>DeprecatedEntry<!>
    A.RegularEntry
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, stringLiteral */
