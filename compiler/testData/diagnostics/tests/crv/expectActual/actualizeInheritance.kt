// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

@file:MustUseReturnValue

expect class Foo() {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
}

fun commonMain() {
    Foo()
    Foo().x()
    Foo().ign()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: BaseFoo.kt

@MustUseReturnValue
open class BaseFoo {
    @IgnorableReturnValue fun x(): String = ""
    fun ign(): String = ""
}

// FILE: jvm.kt

actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>Foo<!> : BaseFoo() {
}

fun main() {
    Foo()
    Foo().x()
    Foo().ign()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
