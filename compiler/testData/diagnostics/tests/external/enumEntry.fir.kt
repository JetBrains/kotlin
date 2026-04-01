// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83104
// LATEST_LV_DIFFERENCE

enum class E {
    <!DEPRECATED_MODIFIER_FOR_TARGET!>external<!> A {
        fun foo() {}
    },

    B,
    <!DEPRECATED_MODIFIER_FOR_TARGET!>external<!> C
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
