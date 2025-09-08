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

open class BaseFoo {
    fun x(): String = ""
    fun ign(): String = "42"
}

// FILE: jvm.kt

// Since BaseFoo is completely Unspecified, we allow actualization without warnings
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>Foo<!> = BaseFoo

fun main() {
    Foo()
    Foo().x()
    Foo().ign()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
