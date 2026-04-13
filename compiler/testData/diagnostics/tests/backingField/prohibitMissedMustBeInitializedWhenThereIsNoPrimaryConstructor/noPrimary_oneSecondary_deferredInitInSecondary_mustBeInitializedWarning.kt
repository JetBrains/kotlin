// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
class Foo {
    <!MUST_BE_INITIALIZED_WARNING!>var x: String<!>
        set(value) {}

    constructor() {
        x = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, propertyDeclaration, secondaryConstructor, setter, stringLiteral */
