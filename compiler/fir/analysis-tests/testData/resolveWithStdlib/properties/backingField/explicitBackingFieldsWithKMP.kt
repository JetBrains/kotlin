// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: a

expect val goodNumbers: List<Int>

<!EXPECT_PROPERTY_WITH_EXPLICIT_BACKING_FIELD!>expect<!> val badNumbers: List<Int>
    field = mutableListOf()

// MODULE: b-jvm()()(a)

actual val goodNumbers: List<Int>
    field = mutableListOf()

actual val badNumbers: List<Int> = listOf(1, 2, 3)

/* GENERATED_FIR_TAGS: actual, expect, integerLiteral, propertyDeclaration */
