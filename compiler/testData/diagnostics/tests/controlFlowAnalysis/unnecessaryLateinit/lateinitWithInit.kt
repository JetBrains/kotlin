// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var bar: String

    init {
        bar = ""
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, init, lateinit, propertyDeclaration, stringLiteral */
