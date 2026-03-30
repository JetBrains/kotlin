// RUN_PIPELINE_TILL: BACKEND
// Issue: KT-49714

// MODULE: common
expect class Counter {
    operator fun inc(): Counter
    operator fun dec(): Counter
}

// MODULE: main()()(common)
actual typealias Counter = Int

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration, operator, typeAliasDeclaration */
