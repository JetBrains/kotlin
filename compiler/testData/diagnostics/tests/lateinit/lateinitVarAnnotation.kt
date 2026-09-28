// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

annotation class Ann

class Foo {
    @Ann
    lateinit var a: String

    @set:Ann
    lateinit var b: String

    @setparam:Ann
    lateinit var c: String

    @get:Ann
    lateinit var d: String

    @field:Ann
    lateinit var e: String

    @property:Ann
    lateinit var f: String

    lateinit var g: String
        @Ann get

    lateinit var h: String
        @Ann set
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
