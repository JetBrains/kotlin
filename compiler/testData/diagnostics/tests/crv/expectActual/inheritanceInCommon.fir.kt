// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

@MustUseReturnValue
expect interface A {
    fun foo(): String
    fun bar(): String
}

interface B : A {
    override fun foo(): String
}

fun testCommon(b: B) {
    b.<!RETURN_VALUE_NOT_USED!>foo<!>()
    b.<!RETURN_VALUE_NOT_USED!>bar<!>()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual interface A {
    actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT!>foo<!>(): String
    actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT!>bar<!>(): String
}

fun testPlatform(b: B) {
    b.<!RETURN_VALUE_NOT_USED!>foo<!>()
    b.bar()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
