// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73945
// WITH_EXTRA_CHECKERS

interface InterfaceFoo {
    <!REDUNDANT_OPEN_IN_INTERFACE!>open<!> val a: Int
}

/* GENERATED_FIR_TAGS: interfaceDeclaration, propertyDeclaration */
