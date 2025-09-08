// LANGUAGE: +MultiPlatformProjects
// RUN_PIPELINE_TILL: BACKEND

// MODULE: m1-common
// FILE: common.kt
@file:MustUseReturnValue

expect class Foo() {
    fun x(): String
    @IgnorableReturnValue fun ign(): String
    val p: Int
}

expect fun toplvl(): String
@IgnorableReturnValue expect fun ignToplvl(): String

fun commonMain() {
    <!RETURN_VALUE_NOT_USED!>toplvl()<!>
    <!RETURN_VALUE_NOT_USED!>Foo()<!>
    <!RETURN_VALUE_NOT_USED!>Foo().x()<!>
    Foo().ign()
    <!RETURN_VALUE_NOT_USED!>Foo().p<!>
    ignToplvl()
}


// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

// <init>, p: MustUse -> MustUse :ok:
// x: MustUse -> ExplicitlyIgnorable :error:
// ign: ExplicitlyIgnorable -> MustUse :error:
@MustUseReturnValue
actual class Foo actual constructor() {
    @IgnorableReturnValue actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun x(): String' defined in 'Foo'; must-use; 'actual fun x(): String' defined in 'Foo'; ignorable")!>x<!>(): String = ""
    actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun ign(): String' defined in 'Foo'; ignorable; 'actual fun ign(): String' defined in 'Foo'; must-use")!>ign<!>(): String = ""
    actual val p: Int = 42
    fun notActual(): String = ""
}

// These two are Unspecified, because annotation is not on file:

actual fun <!ACTUAL_IGNORABILITY_NOT_MATCH_EXPECT("'expect fun toplvl(): String'; must-use; 'actual fun toplvl(): String'; unspecified (implicitly ignorable)")!>toplvl<!>(): String = "" // MustUse -> Unspecified is not allowed
actual fun ignToplvl(): String = "" // Ignorable -> Unspecified is allowed

fun main() {
    toplvl()
    <!RETURN_VALUE_NOT_USED!>Foo()<!>
    Foo().x()
    <!RETURN_VALUE_NOT_USED!>Foo().ign()<!>
    <!RETURN_VALUE_NOT_USED!>Foo().p<!>
    <!RETURN_VALUE_NOT_USED!>Foo().notActual()<!>
    ignToplvl()
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, primaryConstructor, secondaryConstructor */
