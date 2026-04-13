// RUN_PIPELINE_TILL: FRONTEND
enum class E {
    ENTRY
}

class A : E.<!ENUM_ENTRY_AS_TYPE!>ENTRY<!>

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry */
