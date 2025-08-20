// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt

expect class Foo() {
    fun x(): String
    fun ign(): String
    val p: Int
}

expect fun toplvl(): String
expect fun ignToplvl(): String

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


// <init>, x, p: Unspecifed -> MustUse is not allowed
// ign: Unspecified -> ExplicitlyIgnorable is allowed
@MustUseReturnValue
actual class Foo <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT!>actual constructor()<!> {
    actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT!>x<!>(): String = ""
    @IgnorableReturnValue actual fun ign(): String = ""
    actual val <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT!>p<!>: Int = 42
    fun notActual(): String = ""
}

actual fun toplvl(): String = "" // Unspecified -> Unspecified is allowed
@IgnorableReturnValue actual fun ignToplvl(): String = "" // Unspecified -> ExplicitlyIgnorable is allowed

fun main() {
    toplvl()
    <!RETURN_VALUE_NOT_USED!>Foo()<!>
    <!RETURN_VALUE_NOT_USED!>Foo().x()<!>
    Foo().ign()
    <!RETURN_VALUE_NOT_USED!>Foo().p<!>
    <!RETURN_VALUE_NOT_USED!>Foo().notActual()<!>
    ignToplvl()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
