// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

@MustUseReturnValues
expect interface A {
    fun foo(): String
    fun bar(): String
}

interface B : A {
    override fun foo(): String
}

fun testCommon(b: B) {
    b.foo()
    b.bar()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual interface <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>A<!> {
    actual fun foo(): String
    actual fun bar(): String
}

fun testPlatform(b: B) {
    b.foo()
    b.bar()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
