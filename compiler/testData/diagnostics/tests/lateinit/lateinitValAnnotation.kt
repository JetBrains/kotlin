// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

annotation class Ann

class Foo {
    @Ann
    lateinit val a: String

    @set:Ann
    lateinit val b: String

    @setparam:Ann
    lateinit val c: String

    @get:Ann
    lateinit val d: String

    <!INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD!>@field:Ann<!>
    lateinit val e: String

    @property:Ann
    lateinit val f: String

    lateinit val g: String
        @Ann get

    lateinit val h: String
        @Ann set
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
