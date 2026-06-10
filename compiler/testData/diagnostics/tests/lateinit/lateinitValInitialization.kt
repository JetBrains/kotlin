// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

class Foo {
    lateinit val bar: String

    fun inject() {
        bar = ""
    }
}

fun inject(foo: Foo) {
    foo.bar = ""
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
