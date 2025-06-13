// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE:-ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS
open class Foo {
    constructor()

    <!MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING!>open var x: String<!>

    init {
        x = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, propertyDeclaration, secondaryConstructor, stringLiteral */
