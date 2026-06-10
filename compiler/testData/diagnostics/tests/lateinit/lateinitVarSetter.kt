// RUN_PIPELINE_TILL: FRONTEND

class Foo {
    lateinit var a: String
        private set

    private lateinit var b: String
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>public<!> set

    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var c: String
        set(value) {}
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
