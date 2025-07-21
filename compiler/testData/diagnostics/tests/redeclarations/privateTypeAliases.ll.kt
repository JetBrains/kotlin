// LL_FIR_DIVERGENCE
// KTIJ-25422
// LL_FIR_DIVERGENCE

// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KTIJ-25422

// FILE: A1.kt
private typealias A = Int

// FILE: A2.kt
private typealias A = String

/* GENERATED_FIR_TAGS: classDeclaration */
