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
actual class Foo actual constructor() {
    actual fun x(): String = ""
    @IgnorableReturnValue actual fun ign(): String = ""
    actual val p: Int = 42
    fun notActual(): String = ""
}

actual fun toplvl(): String = "" // Unspecified -> Unspecified is allowed
@IgnorableReturnValue actual fun ignToplvl(): String = "" // Unspecified -> ExplicitlyIgnorable is allowed

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
