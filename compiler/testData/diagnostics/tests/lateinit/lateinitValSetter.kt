// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

class Foo {
    lateinit val a: String
        private set

    private lateinit val b: String
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> val c: String
        set(value) {}
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
