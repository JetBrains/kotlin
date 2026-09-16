// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-73945
// WITH_EXTRA_CHECKERS

interface InterfaceFoo {
    <!REDUNDANT_OPEN_IN_INTERFACE!>open<!> val a: Int
}

/* GENERATED_FIR_TAGS: interfaceDeclaration, propertyDeclaration */
