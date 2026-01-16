// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83104

enum class E {
    external A {
        fun foo() {}
    },

    B,
    external C
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
