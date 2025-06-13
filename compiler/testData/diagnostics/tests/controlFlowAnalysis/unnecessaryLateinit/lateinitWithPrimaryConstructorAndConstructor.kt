// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class Foo() {
    lateinit var bar: String

    constructor(baz: Int) : this() {
        bar = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, lateinit, primaryConstructor, propertyDeclaration,
secondaryConstructor, stringLiteral */
