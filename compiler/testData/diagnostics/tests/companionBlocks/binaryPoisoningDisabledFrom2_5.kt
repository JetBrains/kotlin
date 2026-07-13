// RUN_PIPELINE_TILL: BACKEND

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
