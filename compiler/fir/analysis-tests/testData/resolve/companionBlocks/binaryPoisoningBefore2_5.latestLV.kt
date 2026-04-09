// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING
// LANGUAGE_VERSION: 2.4
// LATEST_LV_DIFFERENCE

// MODULE: m1
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: m1.kt
class C {
    companion {
        fun foo() {}
    }
}

// MODULE: m2(m1)
// LANGUAGE: -CompanionBlocksAndExtensions
// FILE: m2.kt
fun test() {
    C()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
