// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : <!CYCLIC_CONSTRUCTOR_DELEGATION_CALL!>this<!>(a) {
        bar = "a"
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, lateinit, propertyDeclaration, secondaryConstructor, stringLiteral */
