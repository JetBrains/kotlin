// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
class Foo {
    constructor()
    <!MUST_BE_INITIALIZED_WARNING!>var x: String<!>
        set(value) {}

    init {
        x = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, propertyDeclaration, secondaryConstructor, setter,
stringLiteral */
