// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -PRE_RELEASE_CLASS
// MODULE: m1
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: m1.kt
open class C

companion fun C.foo() {}
companion val C.bar = 1
companion operator fun C.invoke(s: String) {}

// MODULE: m2(m1)
// LANGUAGE: +CompanionBlocks -CompanionExtensions
// FILE: m2.kt
class D : C() {
    fun test() {
        <!UNRESOLVED_REFERENCE!>foo<!>()
        <!UNRESOLVED_REFERENCE!>bar<!>
    }
}

fun test() {
    C.<!UNRESOLVED_REFERENCE!>foo<!>()
    C.<!UNRESOLVED_REFERENCE!>bar<!>
    C(<!TOO_MANY_ARGUMENTS!>""<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, operator,
propertyDeclaration, propertyWithExtensionReceiver */
