// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

interface Base {
    fun foo()
}

expect open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class Foo<!> : Base

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base {
    final override fun <!EXPECT_ACTUAL_INCOMPATIBLE_MODALITY!>foo<!>() {}
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, interfaceDeclaration, override */
