// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
expect interface Base

// MODULE: m1-jvm()()(m1-common)
actual interface Base {
    override fun <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>equals<!>(other: Any?): Boolean
}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration, interfaceDeclaration, nullableType, operator, override */
