// RUN_PIPELINE_TILL: FRONTEND
enum class A {
    <!REDECLARATION!>name<!>,
    <!REDECLARATION!>ordinal<!>,
    <!DEPRECATED_DECLARATION_OF_ENUM_ENTRY!>entries,<!>
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry */
