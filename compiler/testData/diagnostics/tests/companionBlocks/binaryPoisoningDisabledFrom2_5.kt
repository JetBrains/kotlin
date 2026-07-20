// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -PRE_RELEASE_CLASS

// MODULE: m1
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: m1.kt
class C {
    companion {
        fun foo() {}
    }
}

// MODULE: m2(m1)
// LANGUAGE: -CompanionBlocks -CompanionExtensions
// FILE: m2.kt
fun test() {
    C()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
