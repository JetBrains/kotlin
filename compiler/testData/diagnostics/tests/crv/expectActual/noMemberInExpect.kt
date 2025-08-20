// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

interface A {
    fun x(): String
    fun ign(): String
}

expect interface Impl: A

fun testCommon(i: Impl) {
    i.x()
    i.ign()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@MustUseReturnValue
actual interface Impl: A {
    override fun x(): String
    @IgnorableReturnValue override fun ign(): String
}

fun testPlatform(i: Impl) {
    i.x()
    i.ign()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
