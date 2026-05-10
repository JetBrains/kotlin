// RUN_PIPELINE_TILL: FRONTEND

enum class E {
    ABC {
        <!WRONG_MODIFIER_TARGET!>enum<!> class F {
            DEF
        }
    }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
