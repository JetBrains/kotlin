// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt
@file:MustUseReturnValues

expect class Foo() {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
    val p: Int
}

expect fun toplvl(): String
@IgnorableReturnValue expect fun ignToplvl(): String

fun commonMain() {
    toplvl()
    Foo()
    Foo().x()
    Foo().ign()
    Foo().p
    ignToplvl()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

// <init>, p: MustUse -> MustUse :ok:
// x: MustUse -> ExplicitlyIgnorable :error:
// ign: ExplicitlyIgnorable -> MustUse :error:
@MustUseReturnValues
actual class Foo actual constructor() {
    @IgnorableReturnValue actual fun x(): String = ""
    actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ign<!>(): String = ""
    actual val p: Int = 42
    fun notActual(): String = ""
}

// These two are Unspecified, because annotation is not on file:

actual fun toplvl(): String = "" // MustUse -> Unspecified is not allowed
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ignToplvl<!>(): String = "" // Ignorable -> Unspecified is allowed

fun main() {
    toplvl()
    Foo()
    Foo().x()
    Foo().ign()
    Foo().p
    Foo().notActual()
    ignToplvl()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
