// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83104

enum class E {
    <!WRONG_MODIFIER_TARGET!>external<!> A {
        fun foo() {}
    },

    B,
    <!WRONG_MODIFIER_TARGET!>external<!> C
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
