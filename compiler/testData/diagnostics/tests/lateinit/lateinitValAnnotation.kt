// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals, +CompanionBlocksAndExtensions

annotation class Ann

@Target(AnnotationTarget.FIELD)
annotation class FieldAnn

<!WRONG_ANNOTATION_TARGET!>@FieldAnn<!>
@Ann
lateinit val a: String

@set:Ann
lateinit val b: String

@setparam:Ann
lateinit val c: String

@get:Ann
lateinit val d: String

<!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
lateinit val e: String

@property:Ann
lateinit val f: String

lateinit val g: String
    @Ann get

lateinit val h: String
    @Ann set

class Foo {
    <!WRONG_ANNOTATION_TARGET!>@FieldAnn<!>
    @Ann
    lateinit val a: String

    @set:Ann
    lateinit val b: String

    @setparam:Ann
    lateinit val c: String

    @get:Ann
    lateinit val d: String

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
    lateinit val e: String

    @property:Ann
    lateinit val f: String

    lateinit val g: String
        @Ann get

    lateinit val h: String
        @Ann set

    companion {
        <!WRONG_ANNOTATION_TARGET!>@FieldAnn<!>
        @Ann
        lateinit val a: String

        @set:Ann
        lateinit val b: String

        @setparam:Ann
        lateinit val c: String

        @get:Ann
        lateinit val d: String

        <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@field:Ann<!>
        lateinit val e: String

        @property:Ann
        lateinit val f: String

        lateinit val g: String
            @Ann get

        lateinit val h: String
            @Ann set
    }
}


/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
