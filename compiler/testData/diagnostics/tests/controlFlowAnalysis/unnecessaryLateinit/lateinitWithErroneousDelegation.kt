// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var x: String

    constructor() {
        x = "Foo"
    }

    constructor(x: String, y: String): <!NONE_APPLICABLE!>this<!>(y.hashCode())
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, lateinit, propertyDeclaration, secondaryConstructor, stringLiteral */
