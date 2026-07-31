// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: commonFile.kt
package p

expect class D

expect class D2 {
    override fun equals(@EqualityBound(D2::class) other: Any?): Boolean
}

// MODULE: platform()()(common)
// FILE: platformFile.kt
package p

actual data class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_SCOPE!>D<!>(val d: Int)

actual data class D2(val d2: Int)

/* GENERATED_FIR_TAGS: actual, classDeclaration, data, expect, primaryConstructor, propertyDeclaration */
