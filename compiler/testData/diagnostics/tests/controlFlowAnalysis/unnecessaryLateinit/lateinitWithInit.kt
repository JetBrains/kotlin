// RUN_PIPELINE_TILL: BACKEND
class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    init {
        bar = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, lateinit, propertyDeclaration, stringLiteral */
