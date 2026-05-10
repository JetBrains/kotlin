// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidOperatorEqualsInEnumEntriesAndAnonymousObjects

enum class E {
    X {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(other: E): Boolean = true
    },
    Y {
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(a: Int, b: Int): Unit = Unit
    },
    Z {
        context(e: E)
        <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(): Any? = null
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
