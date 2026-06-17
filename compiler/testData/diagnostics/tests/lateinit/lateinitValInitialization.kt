// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

// MODULE: lib

class Foo {
    lateinit val bar: String

    fun inject() {
        bar = "first"
        bar = "second"
    }
}

fun inject(foo: Foo) {
    foo.bar = "first"
    foo.bar = "second"
}

// MODULE: main(lib)

fun test(foo: Foo) {
    foo.bar = "first"
    foo.bar = "second"
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, lateinit, propertyDeclaration, stringLiteral */
